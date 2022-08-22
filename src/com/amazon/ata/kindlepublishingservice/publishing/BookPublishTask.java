package com.amazon.ata.kindlepublishingservice.publishing;

import com.amazon.ata.kindlepublishingservice.dao.CatalogDao;
import com.amazon.ata.kindlepublishingservice.dao.PublishingStatusDao;
import com.amazon.ata.kindlepublishingservice.dynamodb.models.CatalogItemVersion;
import com.amazon.ata.kindlepublishingservice.enums.PublishingRecordStatus;
import com.amazon.ata.kindlepublishingservice.exceptions.BookNotFoundException;

import javax.inject.Inject;

public class BookPublishTask implements Runnable {

    public BookPublishRequestManager bookPublishRequestManager;
    private PublishingStatusDao publishingStatusDao;
    private CatalogDao catalogDao;

    @Inject
    public BookPublishTask(PublishingStatusDao publishingStatusDao, CatalogDao catalogDao, BookPublishRequestManager bookPublishRequestManager) {
        this.publishingStatusDao = publishingStatusDao;
        this.catalogDao = catalogDao;
        this.bookPublishRequestManager = bookPublishRequestManager;
    }

    public void publish(BookPublishRequestManager bookPublishRequestManager) throws InterruptedException {
        BookPublishRequest request = bookPublishRequestManager.getBookPublishRequestToProcess();
        if (request != null) {
            publishingStatusDao.setPublishingStatus(request.getPublishingRecordId(),
                    PublishingRecordStatus.IN_PROGRESS,
                    request.getBookId());
            KindleFormattedBook kindleFormattedBook = KindleFormatConverter.format(request);
            try {
                CatalogItemVersion itemVersion = catalogDao.createOrUpdateBook(kindleFormattedBook);
                publishingStatusDao.setPublishingStatus(request.getPublishingRecordId(),
                        PublishingRecordStatus.SUCCESSFUL,
                        itemVersion.getBookId());
            } catch (BookNotFoundException e) {
                publishingStatusDao.setPublishingStatus(request.getPublishingRecordId(),
                                                        PublishingRecordStatus.FAILED,
                                                        request.getBookId(),
                                                        "publishing failed");
            }
        }
    }

    @Override
    public void run() {
        try {
            publish(bookPublishRequestManager);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
