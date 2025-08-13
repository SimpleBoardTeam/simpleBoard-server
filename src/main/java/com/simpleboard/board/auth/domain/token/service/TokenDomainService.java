package com.simpleboard.board.auth.domain.token.service;

import com.simpleboard.board.auth.domain.common.vo.Role;
import com.simpleboard.board.auth.domain.token.dto.request.LoginTokenIssueParam;
import com.simpleboard.board.auth.domain.token.dto.request.RefreshTokenRotationParam;
import com.simpleboard.board.auth.domain.token.dto.request.VerifyTokenIssueParam;
import com.simpleboard.board.auth.domain.token.dto.response.LoginTokenInfo;
import com.simpleboard.board.auth.domain.token.dto.response.VerifyTokenInfo;
import com.simpleboard.board.auth.domain.token.exception.*;
import com.simpleboard.board.auth.domain.token.repository.MemberUUIDRepository;
import com.simpleboard.board.auth.domain.token.repository.TokenBlacklistRepository;
import com.simpleboard.board.auth.domain.token.util.*;
import com.simpleboard.board.auth.domain.token.vo.*;
import java.time.Instant;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * <b>TokenDomainService</b> Domain Service & Aggregate Root
 *
 * <p>토큰 발급/검증/로테이션/블랙리스트 등록의 도메인 규칙을 캡슐화
 *
 * <ul>
 *   <li>토큰 발급: Access/Refresh/Verify
 *   <li>리프레시 로테이션 및 블랙리스트 등록
 *   <li>TTL 정책/클록/ID 생성/UUID 매핑/블랙리스트 저장소 협력
 * </ul>
 *
 * @domain domain-service & Aggregate Root
 * @since 1.0
 */
@Service
public class TokenDomainService {

  private final TokenProvider tokenProvider;
  private final TokenTTLPolicy ttlPolicy;
  private final ClockManager clockManager;
  private final IdGenerator idGenerator;
  private final TokenBlacklistRepository blacklistRepository;
  private final MemberUUIDRepository uuidRepository;

  private final String issuer;
  private final String accessAudience;
  private final String refreshAudience;

  public TokenDomainService(
      TokenProvider tokenProvider,
      TokenTTLPolicy ttlPolicy,
      ClockManager clockManager,
      IdGenerator idGenerator,
      TokenBlacklistRepository blacklistRepository,
      MemberUUIDRepository uuidRepository,
      @Value("${app.auth.issuer}") String issuer,
      @Value("${app.auth.audience-access}") String accessAudience,
      @Value("${app.auth.audience-refresh}") String refreshAudience) {
    this.tokenProvider = tokenProvider;
    this.ttlPolicy = ttlPolicy;
    this.clockManager = clockManager;
    this.idGenerator = idGenerator;
    this.blacklistRepository = blacklistRepository;
    this.uuidRepository = uuidRepository;
    this.issuer = issuer;
    this.accessAudience = accessAudience;
    this.refreshAudience = refreshAudience;
  }

  /**
   * <b>로그인 토큰 발급</b>
   *
   * <p>Access/Refresh 토큰을 생성해 한 쌍으로 반환
   *
   * @param command 발급 파라미터(memberId/clientId/role)
   * @return Access/Refresh TokenPair
   * @since 1.0
   */
  public TokenPair issueLoginToken(LoginTokenIssueParam command) {

    Instant now = clockManager.now();
    String uuid = uuidRepository.createOrGetUUID(command.memberId());

    // Create new token pair
    return createLoginTokenPair(uuid, command.role(), now, now);
  }

  /**
   * <b>Verify 토큰 발급</b>
   *
   * <p>email/nickname/password 검증 목적의 토큰 발급
   *
   * @param command 발급 파라미터(subject/purpose/ttl)
   * @return Verify 토큰
   * @since 1.0
   */
  public Token issueVerifyToken(VerifyTokenIssueParam command) {
    Instant now = clockManager.now();
    TokenClaims claims = createVerifyClaims(command.subject(), command.purpose(), now);
    return tokenProvider.issueToken(claims);
  }

  public LoginTokenInfo validateAndParseLoginToken(String token) {

    TokenClaims tokenClaims =
        tokenProvider.verifyAndParseToken(token, Date.from(clockManager.now()));
    if (!(tokenClaims.tokenPurpose().equals(TokenPurpose.ACCESS)
        || tokenClaims.tokenPurpose().equals(TokenPurpose.REFRESH))) throw new TokenTypeException();
    Long memberId =
        uuidRepository
            .getMemberId(tokenClaims.subject())
            .orElseThrow(TokenUserBlockedException::new);
    return createLoginTokenInfo(tokenClaims, memberId);
  }

  public VerifyTokenInfo validateAndParseVerifyToken(String token) {

    TokenClaims tokenClaims =
        tokenProvider.verifyAndParseToken(token, Date.from(clockManager.now()));
    if (tokenClaims.tokenPurpose().equals(TokenPurpose.ACCESS)
        || tokenClaims.tokenPurpose().equals(TokenPurpose.REFRESH)) throw new TokenTypeException();
    return createVerifyTokenInfo(tokenClaims);
  }

  /**
   * <b>리프레시 토큰 로테이션</b>
   *
   * <p>기존 리프레시 토큰을 검증 후 블랙리스트에 등록하고, 새로운 Access/Refresh를 발급
   *
   * @param command 이전 리프레시 토큰/클라이언트 정보
   * @return 새로운 Access/Refresh TokenPair
   * @throws RefreshTokenExpiredException 기존 리프레시 만료
   * @throws RefreshTokenInvalidException 블랙리스트 존재 등 무효
   * @since 1.0
   */
  public TokenPair rotateRefreshToken(RefreshTokenRotationParam command) {
    Instant now = clockManager.now();

    TokenClaims oldClaims =
        tokenProvider.verifyAndParseToken(command.oldRefreshRaw(), Date.from(clockManager.now()));
    if (oldClaims.tokenPurpose() != TokenPurpose.REFRESH) throw new RefreshTokenInvalidException();

    // Validate refresh token
    if (blacklistRepository.exists(oldClaims.tokenId()))
      throw new RefreshTokenInvalidException(); // TODO: 토큰 탈취 이벤트 생성
    uuidRepository.getMemberId(oldClaims.subject()).orElseThrow(TokenUserBlockedException::new);

    // Enroll old token to blacklist
    blacklistRepository.save(oldClaims.tokenId(), oldClaims.expiredAt());

    // Create new token pair
    return createLoginTokenPair(
        oldClaims.subject(), // memberUUID
        oldClaims.role(),
        now,
        oldClaims.issueAt());
  }

  /**
   * <b>리프레시 토큰 블랙리스트 등록</b>
   *
   * <p>검증된 REFRESH 토큰만 블랙리스트에 등록 가능
   *
   * @param refreshTokenRaw 리프레시 토큰 원문
   * @throws RefreshTokenEnrollBlacklistException REFRESH 토큰이 아닌 경우
   * @throws RefreshTokenInvalidException 만료 등으로 무효인 경우
   * @since 1.0
   */
  public void enrollBlacklist(String refreshTokenRaw) {
    TokenClaims claims =
        tokenProvider.verifyAndParseToken(refreshTokenRaw, Date.from(clockManager.now()));

    if (!claims.tokenPurpose().equals(TokenPurpose.REFRESH))
      throw new RefreshTokenEnrollBlacklistException();
    if (claims.isExpired(clockManager.now())) throw new RefreshTokenInvalidException();
    blacklistRepository.save(claims.tokenId(), claims.expiredAt());
  }

  private TokenClaims createAccessClaims(String memberUUID, Role role, Instant now) {
    return TokenClaims.builder()
        .tokenId(idGenerator.newTokenId())
        .tokenPurpose(TokenPurpose.ACCESS)
        .subject(memberUUID)
        .role(role)
        .audience(accessAudience)
        .issuer(issuer)
        .issueAt(now)
        .expiredAt(now.plus(ttlPolicy.accessTtlFor(role)))
        .build();
  }

  private TokenClaims createRefreshClaims(String memberUUID, Role role, Instant now) {
    return TokenClaims.builder()
        .tokenId(idGenerator.newTokenId())
        .tokenPurpose(TokenPurpose.REFRESH)
        .subject(memberUUID)
        .role(role)
        .audience(refreshAudience)
        .issuer(issuer)
        .issueAt(now)
        .expiredAt(now.plus(ttlPolicy.refreshTtlFor(role)))
        .build();
  }

  private TokenClaims createVerifyClaims(String subject, VerifyPurpose purpose, Instant now) {
    return TokenClaims.builder()
        .tokenId(idGenerator.newTokenId())
        .tokenPurpose(TokenPurpose.VERIFY)
        .verifyPurpose(purpose)
        .subject(subject)
        .audience(accessAudience)
        .issuer(issuer)
        .issueAt(now)
        .expiredAt(now.plus(ttlPolicy.verifyTtlFor(purpose)))
        .build();
  }

  private TokenPair createLoginTokenPair(
      String memberUUID, Role role, Instant accessTokenIssueTime, Instant refreshTokenIssueTime) {
    Token accessToken =
        tokenProvider.issueToken(createAccessClaims(memberUUID, role, accessTokenIssueTime));
    Token newRefreshToken =
        tokenProvider.issueToken(createRefreshClaims(memberUUID, role, refreshTokenIssueTime));

    return TokenPair.builder().access(accessToken).refresh(newRefreshToken).build();
  }

  private static LoginTokenInfo createLoginTokenInfo(TokenClaims tokenClaims, Long memberId) {
    return LoginTokenInfo.builder()
        .tokenPurpose(tokenClaims.tokenPurpose())
        .memberId(memberId)
        .role(tokenClaims.role())
        .audience(tokenClaims.audience())
        .issuer(tokenClaims.issuer())
        .issueAt(tokenClaims.issueAt())
        .expiredAt(tokenClaims.expiredAt())
        .build();
  }

  private static VerifyTokenInfo createVerifyTokenInfo(TokenClaims tokenClaims) {
    return VerifyTokenInfo.builder()
        .verifyPurpose(tokenClaims.verifyPurpose())
        .subject(tokenClaims.subject())
        .audience(tokenClaims.audience())
        .issuer(tokenClaims.issuer())
        .issueAt(tokenClaims.issueAt())
        .expiredAt(tokenClaims.expiredAt())
        .build();
  }
}
