package ru.nikidzawa.datingapp.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LikeEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    @JoinColumn(name = "liker_id")
    UserEntity likedUser;

    long likerUserId;

    String content;

    boolean hasText = false;

    boolean isReciprocity = false;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LikeEntity likeEntity = (LikeEntity) o;
        return Objects.equals(id, likeEntity.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}