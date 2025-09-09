package com.simpleboard.board.board.infrastructure.jpa.converter;

import com.simpleboard.board.board.domain.comment.entity.Comment;
import com.simpleboard.board.board.domain.comment.entity.GuestComment;
import com.simpleboard.board.board.domain.comment.entity.MemberComment;
import com.simpleboard.board.board.infrastructure.jpa.entity.CommentEntity;
import com.simpleboard.board.board.infrastructure.jpa.entity.GuestCommentEntity;
import com.simpleboard.board.board.infrastructure.jpa.entity.MemberCommentEntity;
import org.springframework.stereotype.Component;

@Component
public class CommentConverter {

  public CommentEntity toJpaEntity(Comment comment) {
    if (comment instanceof GuestComment) {
      GuestComment guestComment = (GuestComment) comment;
      return GuestCommentEntity.builder()
          .id(comment.getId())
          .parentId(comment.getParentId() == null ? 0L : comment.getParentId())
          .postId(comment.getPostId())
          .content(comment.getContent())
          .commentState(comment.getCommentState())
          .createdAt(comment.getCreatedAt())
          .updatedAt(comment.getUpdatedAt())
          .nickname(guestComment.getNickname())
          .password(guestComment.getPassword())
          .build();
    } else if (comment instanceof MemberComment) {
      MemberComment memberComment = (MemberComment) comment;
      return MemberCommentEntity.builder()
          .id(comment.getId())
          .parentId(comment.getParentId() == null ? 0L : comment.getParentId())
          .postId(comment.getPostId())
          .content(comment.getContent())
          .commentState(comment.getCommentState())
          .createdAt(comment.getCreatedAt())
          .updatedAt(comment.getUpdatedAt())
          .writerId(memberComment.getWriterId())
          .build();
    }
    throw new IllegalArgumentException();
  }

  public Comment toDomainEntity(CommentEntity commentEntity) {
    if (commentEntity instanceof GuestCommentEntity) {
      GuestCommentEntity guestCommentEntity = (GuestCommentEntity) commentEntity;
      return GuestComment.builder()
          .id(commentEntity.getId())
          .parentId(commentEntity.getParentId() == 0 ? null : commentEntity.getParentId())
          .postId(commentEntity.getPostId())
          .content(commentEntity.getContent())
          .commentState(commentEntity.getCommentState())
          .createdAt(commentEntity.getCreatedAt())
          .updatedAt(commentEntity.getUpdatedAt())
          .nickname(guestCommentEntity.getNickname())
          .password(guestCommentEntity.getPassword())
          .build();
    } else if (commentEntity instanceof MemberCommentEntity) {
      MemberCommentEntity memberCommentEntity = (MemberCommentEntity) commentEntity;
      return MemberComment.builder()
          .id(commentEntity.getId())
          .parentId((commentEntity.getParentId() == null  ||
                  commentEntity.getParentId() == 0 )? null : commentEntity.getParentId())
          .postId(commentEntity.getPostId())
          .content(commentEntity.getContent())
          .commentState(commentEntity.getCommentState())
          .createdAt(commentEntity.getCreatedAt())
          .updatedAt(commentEntity.getUpdatedAt())
          .writerId(memberCommentEntity.getWriterId())
          .build();
    }
    throw new IllegalArgumentException();
  }
}
