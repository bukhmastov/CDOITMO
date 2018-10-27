package com.bukhmastov.cdoitmo.model.converter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bukhmastov.cdoitmo.exception.SilentException;
import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.model.JsonEntity;

public abstract class Converter<IN extends JsonEntity, OUT extends JsonEntity> extends ConverterBase {

    private IN entity;

    public Converter(@NonNull IN entity) {
        super();
        this.entity = entity;
    }

    public @Nullable OUT convert() {
        String trace = firebasePerformanceProvider.startTrace(getTraceName());
        try {
            OUT converted = doConvert(entity);
            firebasePerformanceProvider.putAttribute(trace, "state", "done");
            return converted;
        } catch (SilentException silent) {
            firebasePerformanceProvider.putAttribute(trace, "state", "failed");
            return null;
        } catch (Throwable e) {
            firebasePerformanceProvider.putAttribute(trace, "state", "failed");
            firebasePerformanceProvider.putAttribute(trace, "exception", e.getMessage());
            log.get().exception("Failed to convert entity", e);
            return null;
        } finally {
            firebasePerformanceProvider.stopTrace(trace);
        }
    }

    protected abstract OUT doConvert(IN entity) throws Throwable;

    protected abstract @FirebasePerformanceProvider.TRACE String getTraceName();
}
