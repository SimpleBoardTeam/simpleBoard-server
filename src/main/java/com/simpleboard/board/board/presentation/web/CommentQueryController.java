package com.simpleboard.board.board.presentation.web;

import com.simpleboard.board.board.application.dto.response.CommentListQueryResult;
import com.simpleboard.board.board.application.service.CommentQueryService;
import com.simpleboard.board.board.presentation.converter.CommentQueryPresentationApplicationConverter;
import com.simpleboard.board.board.presentation.dto.request.CommentListQueryForm;
import com.simpleboard.board.board.presentation.dto.response.CommentListQueryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Board - Comment API", description = "댓글 Read API")
@RestController
@RequiredArgsConstructor
public class CommentQueryController {

  private final CommentQueryService commentQueryService;
  private final CommentQueryPresentationApplicationConverter converter;

  @Operation(summary = "댓글 목록 조회 API", description = "게시글에 대한 댓글 목록조회를 요청합니다.")
  @GetMapping("board/{boardId}/post/{postId}/comments")
  public ResponseEntity<CommentListQueryResponse> getCommentList(
      @PathVariable Long boardId,
      @PathVariable Long postId,
      @ModelAttribute CommentListQueryForm form) {
    CommentListQueryResult commentList =
        commentQueryService.getCommentList(converter.toQuery(form, postId));
    return ResponseEntity.ok(converter.toQueryResponse(commentList));
  }
}
