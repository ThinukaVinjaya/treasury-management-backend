// EventRequest.java
package com.batch.treasury_management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;

@Data
public class EventRequest {
    @NotBlank
    private String name;

    private String description;

    @NotNull
    private Date startDate;

    private Date endDate;

    private String treasurerId;
}