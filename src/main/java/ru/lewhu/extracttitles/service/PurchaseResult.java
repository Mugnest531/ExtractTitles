package ru.lewhu.extracttitles.service;

public enum PurchaseResult {
    SUCCESS,
    TITLE_NOT_FOUND,
    DISABLED,
    NO_PERMISSION,
    REQUIREMENT_NOT_MET,
    ALREADY_OWNED,
    PROVIDER_UNAVAILABLE,
    NOT_ENOUGH_FUNDS,
    INVALID_DURATION,
    ERROR
}
