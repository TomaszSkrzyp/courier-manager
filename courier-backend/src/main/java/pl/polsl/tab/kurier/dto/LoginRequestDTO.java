package pl.polsl.tab.kurier.dto;

import lombok.Data;

@Data
public class LoginRequestDTO {
    private String login;
    private String password;
}
