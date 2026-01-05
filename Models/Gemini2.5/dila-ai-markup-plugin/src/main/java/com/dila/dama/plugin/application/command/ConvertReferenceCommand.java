package com.dila.dama.plugin.application.command;

import com.dila.dama.plugin.domain.model.InvalidReferenceException;
import com.dila.dama.plugin.domain.model.TripitakaComponents;
import com.dila.dama.plugin.domain.service.ReferenceParser;
import com.dila.dama.plugin.infrastructure.api.CBRDAPIClient;
import com.dila.dama.plugin.infrastructure.api.CBRDAPIException;

public class ConvertReferenceCommand {

    private final ReferenceParser parser;
    private final CBRDAPIClient apiClient;

    public ConvertReferenceCommand(ReferenceParser parser, CBRDAPIClient apiClient) {
        this.parser = parser;
        this.apiClient = apiClient;
    }

    public ConvertReferenceResult execute(String selectedRefXml) {
        try {
            TripitakaComponents parsed = parser.parseReference(selectedRefXml);
            if (parsed == null) {
                return ConvertReferenceResult.failure("error.invalid.xml");
            }
            String url = apiClient.convertToFirstLink(selectedRefXml);
            return ConvertReferenceResult.success(url);
        } catch (InvalidReferenceException e) {
            return ConvertReferenceResult.failure(e.getMessageKey(), e.getParams());
        } catch (CBRDAPIException e) {
            return ConvertReferenceResult.failure(e.getMessageKey(), e.getParams());
        } catch (Exception e) {
            return ConvertReferenceResult.failure("error.api.connection");
        }
    }
}
