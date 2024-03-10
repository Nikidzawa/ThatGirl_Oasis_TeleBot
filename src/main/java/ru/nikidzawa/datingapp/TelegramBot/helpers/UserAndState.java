package ru.nikidzawa.datingapp.TelegramBot.helpers;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import ru.nikidzawa.datingapp.TelegramBot.stateMachine.StateEnum;
import ru.nikidzawa.datingapp.entities.UserEntity;

@Getter
@Setter
@AllArgsConstructor
public class UserAndState {
    UserEntity user;
    StateEnum stateEnum;
    boolean hasBeenRegistered;
}
