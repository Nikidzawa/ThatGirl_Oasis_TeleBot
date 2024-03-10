package ru.nikidzawa.datingapp.TelegramBot.stateMachine;


public enum StateEnum {
    //
    CHECK_GROUP_MEMBER,

    //Регистрация
    START,
    PRE_REGISTER,
    ASK_NAME,
    ASK_AGE,
    ASK_CITY,
    ASK_HOBBY,
    ASK_ABOUT_ME,
    ASK_PHOTO,
    RESULT,

    //Редактирование профиля
    EDIT_PROFILE,
    //
    EDIT_NAME,
    EDIT_AGE,
    //
    EDIT_CITY,
    //
    EDIT_HOBBY,
    EDIT_ABOUT_ME,
    //
    EDIT_PHOTO,
    //
    EDIT_RESULT,

    //Выход из группы или отключение анкеты
    LEFT,
    ASK_BEFORE_OFF,
    WELCOME_BACK,

    //Действия из меню
    MENU,
    PROFILE,
}
