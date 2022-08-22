package com.amazon.ata.kindlepublishingservice.dao;

import com.amazon.ata.kindlepublishingservice.dynamodb.models.CatalogItemVersion;
import com.amazon.ata.kindlepublishingservice.exceptions.BookNotFoundException;
import com.amazon.ata.kindlepublishingservice.models.response.RemoveBookFromCatalogResponse;
import com.amazon.ata.kindlepublishingservice.publishing.KindleFormattedBook;
import com.amazon.ata.kindlepublishingservice.utils.KindlePublishingUtils;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import javax.inject.Inject;

public class CatalogDao {

    private final DynamoDBMapper dynamoDbMapper;

    /**
     * Instantiates a new CatalogDao object.
     *
     * @param dynamoDbMapper The {@link DynamoDBMapper} used to interact with the catalog table.
     */
    @Inject
    public CatalogDao(DynamoDBMapper dynamoDbMapper) {
        this.dynamoDbMapper = dynamoDbMapper;
    }

    /**
     * Returns the latest version of the book from the catalog corresponding to the specified book id.
     * Throws a BookNotFoundException if the latest version is not active or no version is found.
     *
     * @param bookId Id associated with the book.
     * @return The corresponding CatalogItem from the catalog table.
     */
    public CatalogItemVersion getBookFromCatalog(String bookId) {
        CatalogItemVersion book = getLatestVersionOfBook(bookId);

        if (book == null || book.isInactive()) {
            throw new BookNotFoundException(String.format("No book found for id: %s", bookId));
        }

        return book;
    }

    // Returns null if no version exists for the provided bookId
    private CatalogItemVersion getLatestVersionOfBook(String bookId) {
        CatalogItemVersion book = new CatalogItemVersion();
        book.setBookId(bookId);

        DynamoDBQueryExpression<CatalogItemVersion> queryExpression = new DynamoDBQueryExpression()
                .withHashKeyValues(book)
                .withScanIndexForward(false)
                .withLimit(1);

        List<CatalogItemVersion> results = dynamoDbMapper.query(CatalogItemVersion.class, queryExpression);
        if (results.isEmpty()) {
            return null;
        }
        return results.get(0);
    }

    public RemoveBookFromCatalogResponse removeBookFromCatalog(String bookId) {
        CatalogItemVersion bookToRemove = this.getBookFromCatalog(bookId);
        bookToRemove.setInactive(true);
        dynamoDbMapper.save(bookToRemove);
        return null;
    }

    public void validateBookExists(String bookId) {
        CatalogItemVersion book = getLatestVersionOfBook(bookId);

        if (book == null) {
            throw new BookNotFoundException(String.format("No book found for id: %s", bookId));
        }
    }

    public CatalogItemVersion createOrUpdateBook(KindleFormattedBook kindleFormattedBook) {
        CatalogItemVersion itemVersion = new CatalogItemVersion();
        CatalogItemVersion existingVersion;

        if (kindleFormattedBook.getBookId() != null) {
            existingVersion = getLatestVersionOfBook(kindleFormattedBook.getBookId());
            if (existingVersion == null) {
                throw new BookNotFoundException(String.format("No book found for id %s", kindleFormattedBook.getBookId()));
            }
            existingVersion.setInactive(true);
            dynamoDbMapper.save(existingVersion);
            itemVersion.setVersion(existingVersion.getVersion() + 1);
            itemVersion.setBookId(existingVersion.getBookId());
        } else {
            itemVersion.setBookId(KindlePublishingUtils.generateBookId());
            itemVersion.setVersion(1);
        }
        itemVersion.setAuthor(kindleFormattedBook.getAuthor());
        itemVersion.setText(kindleFormattedBook.getText());
        itemVersion.setTitle(kindleFormattedBook.getTitle());
        itemVersion.setGenre(kindleFormattedBook.getGenre());
        itemVersion.setInactive(false);

        dynamoDbMapper.save(itemVersion);
        return itemVersion;
    }
}
