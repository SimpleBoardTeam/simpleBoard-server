package com.simpleboard.board.board.infrastructure.jpa.entity;

import com.simpleboard.board.board.domain.comment.vo.CommentState;
import com.simpleboard.board.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "COMMENT_TYPE")
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "COMMENT",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"PARENT_ID", "SIBLING_SEQ"})})
@SuperBuilder
@Getter
public abstract class CommentEntity extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "COMMENT_ID")
  private Long id;

  private Long parentId;

  @Column(nullable = false)
  private Long postId;

  @Column(nullable = false)
  private String content;

  @Column(nullable = false)
  private CommentState commentState;

  @Setter private Integer siblingSeq;
}
