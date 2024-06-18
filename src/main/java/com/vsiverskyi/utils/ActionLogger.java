package com.vsiverskyi.utils;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ActionLogger {
    private String actionText;
    private LocalDateTime localDateTime;
}
