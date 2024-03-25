package ru.nikidzawa.datingapp.telegramBot.stateMachines.mainStates;


public enum StateEnum {
    //Регистрация
    START,
    START_HANDLE,
    ASK_NAME,
    ASK_AGE,
    ASK_CITY,
    ASK_HOBBY,
    ASK_ABOUT_ME,
    RESULT,

    //Общее для создания и редактирования анкеты
    ASK_AVATAR,

    //Профиль
    MY_PROFILE,
    //
    EDIT_NAME,
    EDIT_AGE,
    //
    EDIT_CITY,
    //
    EDIT_HOBBY,
    EDIT_ABOUT_ME,
    //
    EDIT_RESULT,

    //Выход из группы или отключение анкеты
    LEFT,
    ASK_BEFORE_OFF,
    WELCOME_BACK,
    WELCOME_BACK_HANDLE,
    LIKE_ACCEPT,

    //Просмотр анкет
    FIND_PEOPLES,
    SEND_LIKE_AND_MESSAGE,
    SHOW_WHO_LIKED_ME,
    SHOW_PROFILES_WHO_LIKED_ME,
    STOP_SHOW_PROFILES_WHO_LIKED_ME,

    //меню
    MENU,
    SUPER_MENU,

    //Команды
    FAQ,
    FAQ_RESPONSE,
    SEND_ERROR,
    CALL_BACK_QUERY_COMPLAIN
}
