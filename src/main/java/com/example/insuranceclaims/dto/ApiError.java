package com.example.insuranceclaims.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Uniform error body: {"status":404,"message":"...","fieldErrors":{...}}. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(int status, String message, Map<String, String> fieldErrors) {

    public static ApiError of(int status, String message) {
        return new ApiError(status, message, null);
    }
}
