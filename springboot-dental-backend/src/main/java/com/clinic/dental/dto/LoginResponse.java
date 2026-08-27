package com.clinic.dental.dto;

public record LoginResponse(String token, UserResponse user) {
}
