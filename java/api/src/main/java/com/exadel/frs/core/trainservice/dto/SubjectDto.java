package com.exadel.frs.core.trainservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

import static com.exadel.frs.core.trainservice.system.global.Constants.SUBJECT_DESC;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubjectDto {

    @ApiParam(value = SUBJECT_DESC, required = true)
    @JsonProperty("subject")
    @NotNull
    private String subjectName;
}
