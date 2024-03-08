package ru.nikidzawa.datingapp.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserEntity implements Serializable {

    public UserEntity (Long id) {
        this.id = id;
    }

    @Id
    Long id;

    String name;

    int age;

    String city;

    String hobby;

    String spendYourTime;

    String aboutMe;

    String photo;

    Boolean isActive = false;
}
