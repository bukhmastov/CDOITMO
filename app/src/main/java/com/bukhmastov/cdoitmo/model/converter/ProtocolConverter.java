package com.bukhmastov.cdoitmo.model.converter;

import androidx.annotation.NonNull;

import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.model.protocol.PChange;
import com.bukhmastov.cdoitmo.model.protocol.Protocol;
import com.bukhmastov.cdoitmo.model.protocol.hash.PHash;
import com.bukhmastov.cdoitmo.util.Storage;
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;
import com.bukhmastov.cdoitmo.util.singleton.NumberUtils;
import com.bukhmastov.cdoitmo.util.singleton.StringUtils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProtocolConverter extends Converter<Protocol, Protocol> {

    public ProtocolConverter(@NonNull Protocol entity) {
        super(entity);
    }

    @Override
    protected Protocol doConvert(Protocol protocol) throws Throwable {
        if (protocol == null) {
            return null;
        }
        List<PChange> changes = protocol.getChanges();
        if (CollectionUtils.isEmpty(changes)) {
            return protocol;
        }
        for (PChange change : changes) {
            if (change == null) {
                continue;
            }
            change.setValue(markConverter(change.getValue()));
            change.setMin(markConverter(change.getMin()));
            change.setMax(markConverter(change.getMax()));
            change.setThreshold(markConverter(change.getThreshold()));
            String hash = StringUtils.crypt(makeSignature(change));
            Double value, oldValue, delta, oldDelta;
            if (storagePref.get().get(context.get(), "pref_protocol_changes_track_title", true)) {
                String hashContent = storage.get().get(context.get(), Storage.CACHE, Storage.USER, "protocol#log#" + hash, null);
                if (StringUtils.isBlank(hashContent)) {
                    oldValue = null;
                    oldDelta = null;
                } else {
                    try {
                        PHash pHash = new PHash().fromJsonString(hashContent);
                        oldValue = pHash.getValue();
                        oldDelta = pHash.getDelta();
                    } catch (Exception e) {
                        oldValue = null;
                        oldDelta = null;
                    }
                }
                value = NumberUtils.toDouble(change.getValue());
                delta = 0.0;
                if (value != null) {
                    if (oldValue != null) {
                        delta = value - oldValue;
                        if (delta == 0.0) {
                            delta = oldDelta;
                        }
                    }
                    PHash pHash = new PHash();
                    pHash.setValue(value);
                    pHash.setDelta(delta);
                    storage.get().put(context.get(), Storage.CACHE, Storage.USER, "protocol#log#" + hash, pHash.toJsonString());
                }
            } else {
                delta = 0.0;
            }
            delta = round(delta);
            change.setCdoitmoHash(hash);
            change.setCdoitmoDelta(markConverter(String.valueOf(delta), true));
            change.setCdoitmoDeltaDouble(delta);
        }
        return protocol;
    }

    @Override
    protected String getTraceName() {
        return FirebasePerformanceProvider.Trace.Convert.PROTOCOL;
    }

    private String makeSignature(PChange change) {
        final String separator = "#";
        return (new StringBuilder())
                .append(change.getSubject()).append(separator)
                .append(change.getName()).append(separator)
                .append(change.getMin()).append(separator)
                .append(change.getMax()).append(separator)
                .append(change.getThreshold()).append(separator)
                .toString();
    }

    private String markConverter(String value) {
        return markConverter(value, false);
    }

    private String markConverter(String value, boolean withSign) {
        Matcher m;
        value = value.replace(",", ".").trim();
        m = Pattern.compile("^\\.(.+)").matcher(value);
        if (m.find()) value = "0." + m.group(1);
        m = Pattern.compile("(.+)\\.0$").matcher(value);
        if (m.find()) value = m.group(1);
        if (withSign) {
            m = Pattern.compile("^(\\D?)(\\d*\\.?\\d*)$").matcher(value);
            if (m.find() && m.group(1).isEmpty()) {
                value = "+" + m.group(2);
            }
        }
        return value;
    }

    private double round(double d) {
        int precise = 100;
        d = d * precise;
        int i = (int) Math.round(d);
        return (double) i / precise;
    }
}
