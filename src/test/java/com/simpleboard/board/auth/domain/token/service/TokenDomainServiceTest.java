package com.simpleboard.board.auth.domain.token.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.simpleboard.board.auth.domain.common.vo.Role;
import com.simpleboard.board.auth.domain.token.dto.request.LoginTokenIssueParam;
import com.simpleboard.board.auth.domain.token.dto.request.RefreshTokenRotationParam;
import com.simpleboard.board.auth.domain.token.dto.request.VerifyTokenIssueParam;
import com.simpleboard.board.auth.domain.token.exception.RefreshTokenEnrollBlacklistException;
import com.simpleboard.board.auth.domain.token.exception.RefreshTokenExpiredException;
import com.simpleboard.board.auth.domain.token.exception.RefreshTokenInvalidException;
import com.simpleboard.board.auth.domain.token.repository.MemberUUIDRepository;
import com.simpleboard.board.auth.domain.token.repository.TokenBlacklistRepository;
import com.simpleboard.board.auth.domain.token.util.ClockManager;
import com.simpleboard.board.auth.domain.token.util.IdGenerator;
import com.simpleboard.board.auth.domain.token.util.TokenProvider;
import com.simpleboard.board.auth.domain.token.vo.Token;
import com.simpleboard.board.auth.domain.token.vo.TokenClaims;
import com.simpleboard.board.auth.domain.token.vo.TokenPair;
import com.simpleboard.board.auth.domain.token.vo.TokenPurpose;
import com.simpleboard.board.auth.domain.token.vo.VerifyPurpose;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TokenDomainServiceTest {

  private TokenProvider tokenProvider;
  private TokenTTLPolicy ttlPolicy;
  private ClockManager clockManager;
  private IdGenerator idGenerator;
  private TokenBlacklistRepository blacklistRepository;
  private MemberUUIDRepository uuidRepository;
  private TokenDomainService service;

  @BeforeEach
  void setUp() {
    tokenProvider = mock(TokenProvider.class);
    ttlPolicy = mock(TokenTTLPolicy.class);
    clockManager = mock(ClockManager.class);
    idGenerator = mock(IdGenerator.class);
    blacklistRepository = mock(TokenBlacklistRepository.class);
    uuidRepository = mock(MemberUUIDRepository.class);
    service =
        new TokenDomainService(
            tokenProvider,
            ttlPolicy,
            clockManager,
            idGenerator,
            blacklistRepository,
            uuidRepository,
            "issuer",
            "accessAudience",
            "refreshAudience");
  }

  @Test
  @DisplayName("로그인 토큰 발급 성공 테스트")
  void loginToken_Issue_Success_Test() {
    // given
    Long memberId = 42L;
    Role role = Role.MEMBER;
    Instant now = Instant.now();
    when(clockManager.now()).thenReturn(now);
    when(idGenerator.newTokenId()).thenReturn("accessId", "refreshId");
    when(uuidRepository.createOrGetUUID(memberId)).thenReturn("uuid");
    when(ttlPolicy.accessTtlFor(role)).thenReturn(Duration.ofMinutes(10));
    when(ttlPolicy.refreshTtlFor(role)).thenReturn(Duration.ofDays(30));
    Token dummyAccess =
        Token.builder()
            .raw("a")
            .tokenId("accessId")
            .subject("uuid")
            .expiredAt(now.plus(Duration.ofMinutes(10)))
            .build();
    Token dummyRefresh =
        Token.builder()
            .raw("r")
            .tokenId("refreshId")
            .subject("uuid")
            .expiredAt(now.plus(Duration.ofDays(30)))
            .build();
    when(tokenProvider.issueToken(any(TokenClaims.class))).thenReturn(dummyAccess, dummyRefresh);

    LoginTokenIssueParam cmd = LoginTokenIssueParam.builder().memberId(memberId).role(role).build();

    // when
    TokenPair result = service.issueLoginToken(cmd);

    // then
    assertThat(result).isNotNull();
    assertThat(result.access()).isEqualTo(dummyAccess);
    assertThat(result.refresh()).isEqualTo(dummyRefresh);
  }

  @Test
  @DisplayName("Verify 토큰 발급 성공 테스트")
  void verifyToken_Issue_Success_Test() {
    // given
    String subject = "email@example.com";
    VerifyPurpose purpose = VerifyPurpose.EMAIL;
    Duration ttl = Duration.ofMinutes(5);
    Instant now = Instant.now();
    when(clockManager.now()).thenReturn(now);
    when(idGenerator.newTokenId()).thenReturn("verifyId");
    when(ttlPolicy.verifyTtlFor(purpose)).thenReturn(ttl);
    Token dummy =
        Token.builder()
            .raw("v")
            .tokenId("verifyId")
            .subject(subject)
            .expiredAt(now.plus(ttl))
            .build();
    when(tokenProvider.issueToken(any(TokenClaims.class))).thenReturn(dummy);

    VerifyTokenIssueParam cmd = new VerifyTokenIssueParam(purpose, subject, ttl);

    // when
    Token result = service.issueVerifyToken(cmd);

    // then
    assertThat(result).isEqualTo(dummy);
  }

  @Test
  @DisplayName("리프레시 로테이션 성공 테스트")
  void rotateRefreshToken_Success_Test() {
    // given
    String oldRaw = "oldRaw";
    Instant now = Instant.now();
    TokenClaims oldClaims =
        TokenClaims.builder()
            .tokenId("oldId")
            .tokenPurpose(TokenPurpose.REFRESH)
            .subject("uuid")
            .role(Role.ADMIN)
            .issueAt(now.minusSeconds(1))
            .expiredAt(now.plusSeconds(100))
            .audience("refreshAudience")
            .issuer("issuer")
            .build();
    when(clockManager.now()).thenReturn(now);
    when(tokenProvider.verifyAndParseToken(oldRaw, Date.from(now))).thenReturn(oldClaims);
    when(blacklistRepository.exists("oldId")).thenReturn(false);
    doNothing().when(blacklistRepository).save("oldId", oldClaims.expiredAt());
    when(uuidRepository.getMemberId("uuid")).thenReturn(Optional.of(99L));
    when(idGenerator.newTokenId()).thenReturn("newAccessId", "newRefreshId");
    when(ttlPolicy.accessTtlFor(Role.ADMIN)).thenReturn(Duration.ofMinutes(5));
    when(ttlPolicy.refreshTtlFor(Role.ADMIN)).thenReturn(Duration.ofDays(15));
    Token dummyA =
        Token.builder()
            .raw("na")
            .tokenId("newAccessId")
            .subject("uuid")
            .expiredAt(now.plus(Duration.ofMinutes(5)))
            .build();
    Token dummyR =
        Token.builder()
            .raw("nr")
            .tokenId("newRefreshId")
            .subject("uuid")
            .expiredAt(oldClaims.expiredAt().plus(Duration.ofDays(15)))
            .build();
    when(tokenProvider.issueToken(any(TokenClaims.class))).thenReturn(dummyA, dummyR);

    RefreshTokenRotationParam cmd =
        RefreshTokenRotationParam.builder().oldRefreshRaw(oldRaw).build();

    // when
    TokenPair result = service.rotateRefreshToken(cmd);

    // then
    assertThat(result.access()).isEqualTo(dummyA);
    assertThat(result.refresh()).isEqualTo(dummyR);
    verify(blacklistRepository).save("oldId", oldClaims.expiredAt());
  }

  @Test
  @DisplayName("리프레시 로테이션 - 잘못된 토큰 목적 실패 테스트")
  void rotateRefreshToken_InvalidPurpose_Fail_Test() {
    // given
    String oldRaw = "oldRaw";
    Instant now = Instant.now();
    TokenClaims oldClaims =
        TokenClaims.builder()
            .tokenId("id")
            .tokenPurpose(TokenPurpose.ACCESS)
            .subject("sub")
            .issueAt(now)
            .expiredAt(now.plusSeconds(100))
            .build();
    when(clockManager.now()).thenReturn(now);
    when(tokenProvider.verifyAndParseToken(oldRaw, Date.from(now))).thenReturn(oldClaims);

    RefreshTokenRotationParam cmd =
        RefreshTokenRotationParam.builder().oldRefreshRaw(oldRaw).build();

    // when & then
    assertThatThrownBy(() -> service.rotateRefreshToken(cmd))
        .isInstanceOf(RefreshTokenInvalidException.class);
  }

  @Test
  @DisplayName("리프레시 로테이션 - 토큰 만료 실패 테스트")
  void rotateRefreshToken_Expired_Fail_Test() {
    // given
    String oldRaw = "oldRaw";
    Instant now = Instant.now();
    TokenClaims oldClaims =
        TokenClaims.builder()
            .tokenId("id")
            .tokenPurpose(TokenPurpose.REFRESH)
            .subject("sub")
            .issueAt(now.minusSeconds(200))
            .expiredAt(now.minusSeconds(10))
            .build();
    when(clockManager.now()).thenReturn(now);
    when(tokenProvider.verifyAndParseToken(oldRaw, Date.from(now))).thenReturn(oldClaims);

    RefreshTokenRotationParam cmd =
        RefreshTokenRotationParam.builder().oldRefreshRaw(oldRaw).build();

    // when & then
    assertThatThrownBy(() -> service.rotateRefreshToken(cmd))
        .isInstanceOf(RefreshTokenExpiredException.class);
  }

  @Test
  @DisplayName("리프레시 로테이션 - 블랙리스트 로우 존재 실패 테스트")
  void rotateRefreshToken_Blacklisted_Fail_Test() {
    // given
    String oldRaw = "oldRaw";
    Instant now = Instant.now();
    TokenClaims oldClaims =
        TokenClaims.builder()
            .tokenId("id")
            .tokenPurpose(TokenPurpose.REFRESH)
            .subject("sub")
            .issueAt(now)
            .expiredAt(now.plusSeconds(100))
            .build();
    when(clockManager.now()).thenReturn(now);
    when(tokenProvider.verifyAndParseToken(oldRaw, Date.from(now))).thenReturn(oldClaims);
    when(blacklistRepository.exists("id")).thenReturn(true);

    RefreshTokenRotationParam cmd =
        RefreshTokenRotationParam.builder().oldRefreshRaw(oldRaw).build();

    // when & then
    assertThatThrownBy(() -> service.rotateRefreshToken(cmd))
        .isInstanceOf(RefreshTokenInvalidException.class);
  }

  @Test
  @DisplayName("블랙리스트 등록 성공 테스트")
  void enrollBlacklist_Success_Test() {
    // given
    String raw = "raw";
    Instant now = Instant.now();
    TokenClaims claims =
        TokenClaims.builder()
            .tokenId("id")
            .tokenPurpose(TokenPurpose.REFRESH)
            .subject("sub")
            .issueAt(now.minusSeconds(1))
            .expiredAt(now.plusSeconds(100))
            .build();
    when(clockManager.now()).thenReturn(now);
    when(tokenProvider.verifyAndParseToken(raw, Date.from(now))).thenReturn(claims);
    doNothing().when(blacklistRepository).save("id", claims.expiredAt());

    // when
    service.enrollBlacklist(raw);

    // then
    verify(blacklistRepository).save("id", claims.expiredAt());
  }

  @Test
  @DisplayName("블랙리스트 등록 - 잘못된 목적 실패 테스트")
  void enrollBlacklist_InvalidPurpose_Fail_Test() {
    // given
    Instant now = Instant.now();
    String raw = "raw";
    TokenClaims claims =
        TokenClaims.builder()
            .tokenId("id")
            .tokenPurpose(TokenPurpose.ACCESS)
            .subject("sub")
            .issueAt(Instant.now())
            .expiredAt(Instant.now().plusSeconds(100))
            .build();
    when(clockManager.now()).thenReturn(now);
    when(tokenProvider.verifyAndParseToken(raw, Date.from(now))).thenReturn(claims);

    // when & then
    assertThatThrownBy(() -> service.enrollBlacklist(raw))
        .isInstanceOf(RefreshTokenEnrollBlacklistException.class);
  }

  @Test
  @DisplayName("블랙리스트 등록 - 만료된 토큰 실패 테스트")
  void enrollBlacklist_Expired_Fail_Test() {
    // given
    String raw = "raw";
    Instant now = Instant.now();
    TokenClaims claims =
        TokenClaims.builder()
            .tokenId("id")
            .tokenPurpose(TokenPurpose.REFRESH)
            .subject("sub")
            .issueAt(now.minusSeconds(200))
            .expiredAt(now.minusSeconds(10))
            .build();
    when(clockManager.now()).thenReturn(now);
    when(tokenProvider.verifyAndParseToken(raw, Date.from(now))).thenReturn(claims);

    // when & then
    assertThatThrownBy(() -> service.enrollBlacklist(raw))
        .isInstanceOf(RefreshTokenInvalidException.class);
  }
}
