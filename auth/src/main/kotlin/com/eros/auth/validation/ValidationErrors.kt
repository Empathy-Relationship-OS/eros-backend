package com.eros.auth.validation

enum class Errors(val message: String) {
    NULL("Password must not be null."),
    EMPTY("Password must not be empty."),
    WHITESPACE("Password must not contain whitespace!"),
    LENGTH("Must be at least ${PasswordValidator.MIN_LENGTH} characters."),
    UPPER_MISSING("Missing an uppercase letter."),
    LOWER_MISSING("Missing a lowercase letter."),
    DIGIT_MISSING("Missing a digit."),
    SPECIAL_MISSING("Missing a special character."),

    PHONE_NULL("Phone number must not be null!"),
    PHONE_PLUS("Phone number must start with a '+'!"),
    PHONE_EMPTY("Phone number must not be empty!"),
    PHONE_DIGITS("Phone number must contain only digits!"),
    PHONE_ZERO("Phone number must not start with a '0'!"),
    PHONE_LONG("Phone number must be less than ${PhoneValidator.MAX_DIGITS} characters!"),

    EMAIL_NULL("Email is null."),
    EMAIL_EMPTY("Email is empty."),
    EMAIL_INVALID("Not a valid email."),

    UNDERAGE("User must be at least 18 years old."),
    DATE_FORMATTING("Invalid birth date format. Expected ISO-8601 format (YYYY-MM-DD)"),
    BLANK_NAME("Name cannot be blank"),

    OTP_BLANK("OTP cannot be blank"),
    OTP_NON_DIGITS("OTP must contain only digits"),
    OTP_SIZE("OTP must be between ${OTPValidator.MIN_DIGITS} and ${OTPValidator.MAX_DIGITS} digits"),
}