/*
 * Copyright (c) 2012 - 2015, Internet Corporation for Assigned Names and
 * Numbers (ICANN) and China Internet Network Information Center (CNNIC)
 * 
 * All rights reserved.
 *  
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *  
 * * Redistributions of source code must retain the above copyright notice,
 *  this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *  this list of conditions and the following disclaimer in the documentation
 *  and/or other materials provided with the distribution.
 * * Neither the name of the ICANN, CNNIC nor the names of its contributors may
 *  be used to endorse or promote products derived from this software without
 *  specific prior written permission.
 *  
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL ICANN OR CNNIC BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.restfulwhois.rdap.common.service;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.restfulwhois.rdap.common.dao.UpdateDao;
import org.restfulwhois.rdap.common.dto.BaseDto;
import org.restfulwhois.rdap.common.dto.UpdateResponse;
import org.restfulwhois.rdap.common.model.Event;
import org.restfulwhois.rdap.common.model.base.BaseModel;
import org.restfulwhois.rdap.common.util.JsonUtil;
import org.restfulwhois.rdap.common.util.UpdateValidateUtil;
import org.restfulwhois.rdap.common.validation.UpdateValidationError;
import org.restfulwhois.rdap.common.validation.ValidationError;
import org.restfulwhois.rdap.common.validation.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * abstract update service.
 * 
 * @author jiashuo
 * 
 */
public abstract class AbstractUpdateService<DTO extends BaseDto, MODEL extends BaseModel>
        implements UpdateService<DTO, MODEL> {
    /**
     * logger.
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(AbstractUpdateService.class);

    @Autowired
    protected UpdateDao<MODEL> dao;

    @Override
    public UpdateResponse execute(DTO dto) {
        LOGGER.info("begin update dto:{}", dto);
        long queryStart = System.currentTimeMillis();
        ValidationResult validationResult = validate(dto);
        if (validationResult.hasError()) {
            return handleError(dto, validationResult);
        }
        MODEL model = convertDtoToModel(dto);
        execute(model);
        UpdateResponse response =
                UpdateResponse.buildSuccessResponse(model.getHandle());
        long usedTime = System.currentTimeMillis() - queryStart;
        LOGGER.info("end update, milliseconds:{}", usedTime);
        return response;
    }

    abstract protected void execute(MODEL model);

    abstract protected MODEL convertDtoToModel(DTO dto);

    abstract protected ValidationResult validate(DTO dto);

    protected void convertCustomProperties(DTO dto, MODEL model) {
        Map<String, String> customProperties = dto.getCustomProperties();
        model.setCustomProperties(customProperties);
        model.setCustomPropertiesJsonVal(JsonUtil
                .serializeMap(customProperties));
    }

    protected void checkNotEmpty(String value, String fieldName,
            ValidationResult validationResult) {
        if (validationResult.hasError()) {
            return;
        }
        UpdateValidateUtil.checkNotEmpty(value, fieldName, validationResult);
    }

    protected void checkMaxLength(String value, int maxLength,
            String fieldName, ValidationResult validationResult) {
        if (validationResult.hasError()) {
            return;
        }
        UpdateValidateUtil.checkMaxLength(value, maxLength, fieldName,
                validationResult);
    }

    protected void checkNotEmptyAndMaxLength(String value, int maxLength,
            String fieldName, ValidationResult validationResult) {
        checkNotEmpty(value, fieldName, validationResult);
        checkMaxLength(value, maxLength, fieldName, validationResult);
    }

    protected void checkMinMaxInt(int value, int minValue, long maxValue,
            String fieldName, ValidationResult validationResult) {
        UpdateValidateUtil.checkMinMaxInt(value, minValue, maxValue, fieldName,
                validationResult);
    }

    protected void checkMinMaxDate(Date value, Date minValue, Date maxValue,
            String fieldName, ValidationResult validationResult) {
        UpdateValidateUtil.checkMinMaxDate(value, minValue, maxValue,
                fieldName, validationResult);
    }

    protected void checkEvents(List<Event> events,
            ValidationResult validationResult) {
        if (null == events || events.isEmpty()) {
            return;
        }
        for (Event event : events) {
            checkMinMaxDate(
                    null,
                    // event.getEventDate(),
                    UpdateValidateUtil.MIN_VAL_FOR_TIMESTAMP_COLUMN,
                    UpdateValidateUtil.MAX_VAL_FOR_TIMESTAMP_COLUMN,
                    "event.eventDate", validationResult);
        }
    }

    protected void checkHandleNotExistForCreate(String handle,
            ValidationResult validationResult) {
        if (validationResult.hasError()) {
            return;
        }
        Long id = dao.findIdByHandle(handle);
        if (null != id) {
            validationResult.addError(UpdateValidationError
                    .build4091Error(handle));
        }
    }

    protected void checkHandleExistForUpdate(String handle,
            ValidationResult validationResult) {
        if (validationResult.hasError()) {
            return;
        }
        Long id = dao.findIdByHandle(handle);
        if (null == id) {
            validationResult.addError(UpdateValidationError
                    .build4041Error(handle));
        }
    }

    private UpdateResponse handleError(BaseDto dto,
            ValidationResult validationResult) {
        ValidationError error = validationResult.getFirstError();
        UpdateValidationError validationError = (UpdateValidationError) error;
        return UpdateResponse.buildErrorResponse(dto.getHandle(),
                validationError.getCode(), validationError.getHttpStatusCode(),
                validationError.getMessage());
    }
}
