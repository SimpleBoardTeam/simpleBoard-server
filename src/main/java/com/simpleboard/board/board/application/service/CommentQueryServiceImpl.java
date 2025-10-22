package com.simpleboard.board.board.application.service;

import com.simpleboard.board.board.application.converter.CommentQueryServiceRepoConverter;
import com.simpleboard.board.board.application.dto.request.CommentListQuery;
import com.simpleboard.board.board.application.dto.response.AuthorSummary;
import com.simpleboard.board.board.application.dto.response.CommentListQueryResult;
import com.simpleboard.board.board.application.query.CommentListReadModel;
import com.simpleboard.board.board.application.query.CommentQueryRepository;
import com.simpleboard.board.board.domain.post.vo.PostTypeEnum;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommentQueryServiceImpl implements CommentQueryService {
  private final CommentQueryRepository commentQueryRepository;
  private final CommentQueryServiceRepoConverter converter;
  private final MemberFetchService memberFetchService;

  /**
   * <b>Comment 목록 조회 USECASE</b>
   *
   * <ul>
   *   <li>QueryRepository에서 Comment 목록 조회 수행
   *   <li>MemberComment의 nickname을 Member B.C에서 조회 후 매핑
   * </ul>
   *
   * @since 1.0
   */
  @Override
  public CommentListQueryResult getCommentList(CommentListQuery query) {
    CommentListReadModel readModel =
        commentQueryRepository.getCommentList(converter.getCriteria(query));
    List<Long> writerIds =
        readModel.comments().stream()
            .filter(c -> c.commentType().equals(PostTypeEnum.MEMBER))
            .map(c -> c.writerId())
            .toList();
    Map<Long, String> nicknameMap =
        memberFetchService.fetchAuthorSummaryList(writerIds).stream()
            .collect(Collectors.toMap(AuthorSummary::authorId, summary -> summary.nickname()));

    return converter.getQueryResult(readModel, nicknameMap);
  }
}
