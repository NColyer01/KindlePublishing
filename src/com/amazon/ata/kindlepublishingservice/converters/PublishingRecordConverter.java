package com.amazon.ata.kindlepublishingservice.converters;

import com.amazon.ata.kindlepublishingservice.dynamodb.models.PublishingStatusItem;
import com.amazon.ata.kindlepublishingservice.models.PublishingStatusRecord;

import java.sql.Array;
import java.util.ArrayList;
import java.util.List;

public class PublishingRecordConverter {

    public static List<PublishingStatusRecord> convert(List<PublishingStatusItem> publishingStatusItems) {
        List<PublishingStatusRecord> result = new ArrayList<>();
        for(PublishingStatusItem item : publishingStatusItems) {
            PublishingStatusRecord record = PublishingStatusRecord.builder()
                    .withBookId(item.getBookId())
                    .withStatusMessage(item.getStatusMessage())
                    .withStatus(item.getStatus().toString())
                    .build();
            result.add(record);
        }
        return result;
    }
}
