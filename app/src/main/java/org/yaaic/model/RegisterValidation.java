package org.yaaic.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by ping on 7/05/15.
 */
public class RegisterValidation {

    @JsonIgnoreProperties(ignoreUnknown = true)
    private int code;

    @JsonIgnoreProperties(ignoreUnknown = true)
    private boolean valid;

    @JsonIgnoreProperties(ignoreUnknown = true)
    private String message;

    @JsonIgnoreProperties(ignoreUnknown = true)
    private ArrayList<String> suggestion;

    @JsonIgnoreProperties(ignoreUnknown = true)
    private HashMap<String,Object> errors;

    public RegisterValidation() {
    }

    public HashMap<String, Object> getErrors() {
        return errors;
    }

    public void setErrors(HashMap<String, Object> errors) {
        this.errors = errors;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public ArrayList<String> getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(ArrayList<String> suggestion) {
        this.suggestion = suggestion;
    }
}
