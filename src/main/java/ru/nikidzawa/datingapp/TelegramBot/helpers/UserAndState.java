package ru.nikidzawa.datingapp.TelegramBot.helpers;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import ru.nikidzawa.datingapp.TelegramBot.stateMachines.states.StateEnum;
import ru.nikidzawa.datingapp.store.entities.user.UserEntity;

@Getter
@Setter
@AllArgsConstructor
public class UserAndState {
    UserEntity user;
    StateEnum stateEnum;
    boolean hasBeenRegistered;
}
