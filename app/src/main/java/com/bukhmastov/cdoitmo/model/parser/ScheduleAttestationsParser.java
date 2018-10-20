package com.bukhmastov.cdoitmo.model.parser;

import android.support.annotation.NonNull;

import com.bukhmastov.cdoitmo.exception.SilentException;
import com.bukhmastov.cdoitmo.firebase.FirebasePerformanceProvider;
import com.bukhmastov.cdoitmo.model.schedule.attestations.SAttestation;
import com.bukhmastov.cdoitmo.model.schedule.attestations.SAttestations;
import com.bukhmastov.cdoitmo.model.schedule.attestations.SSubject;
import com.bukhmastov.cdoitmo.util.singleton.CollectionUtils;

import org.htmlcleaner.TagNode;

import java.util.ArrayList;

public class ScheduleAttestationsParser extends Parser<SAttestations> {

    private final int term;

    public ScheduleAttestationsParser(@NonNull String html, int term) {
        super(html);
        this.term = term;
    }

    @Override
    protected SAttestations doParse(TagNode root) throws Throwable {
        TagNode[] content = root.getElementsByAttValue("class", "c-page", true, false);
        if (content == null || content.length == 0) {
            throw new SilentException();
        }
        TagNode[] titles = content[0].getElementsByName("h4", true);
        TagNode[] tables = content[0].getElementsByName("table", true);
        if (titles == null || tables == null) {
            throw new SilentException();
        }
        final int length = Math.min(titles.length, tables.length);
        SAttestations schedule = new SAttestations();
        schedule.setSchedule(new ArrayList<>());
        for (int i = 0; i < length; i++) {
            try {
                String sbj = titles[i].getText().toString().trim();
                TagNode[] trs = tables[i].getElementsByName("tr", true);
                ArrayList<SAttestation> attestations = new ArrayList<>();
                for (TagNode tr : trs) {
                    final TagNode[] tds = tr.getElementsByName("td", true);
                    if (tds.length < 2) continue;
                    SAttestation attestation = new SAttestation();
                    attestation.setName(tds[0].getText().toString().trim());
                    attestation.setWeek(tds[1].getText().toString().trim());
                    attestations.add(attestation);
                }
                if (CollectionUtils.isNotEmpty(attestations)) {
                    SSubject subject = new SSubject();
                    subject.setName(sbj);
                    subject.setTerm(term);
                    subject.setAttestations(attestations);
                    schedule.getSchedule().add(subject);
                }
            } catch (Exception e) {
                log.exception(e);
            }
        }
        return schedule;
    }

    @Override
    protected String getTraceName() {
        return FirebasePerformanceProvider.Trace.Parse.SCHEDULE_ATTESTATIONS;
    }
}
