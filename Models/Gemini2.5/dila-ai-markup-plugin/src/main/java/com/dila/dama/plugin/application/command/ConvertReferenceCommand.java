package com.dila.dama.plugin.application.command;

import com.dila.dama.plugin.domain.model.InvalidReferenceException;
import com.dila.dama.plugin.domain.model.TransformationException;
import com.dila.dama.plugin.domain.model.TransformedComponents;
import com.dila.dama.plugin.domain.model.TripitakaComponents;
import com.dila.dama.plugin.domain.service.ComponentTransformer;
import com.dila.dama.plugin.domain.service.ReferenceParser;
import com.dila.dama.plugin.infrastructure.api.CBRDAPIClient;
import com.dila.dama.plugin.infrastructure.api.CBRDAPIException;

public class ConvertReferenceCommand {

    private final ReferenceParser parser;
    private final ComponentTransformer transformer;
    private final CBRDAPIClient apiClient;

    public ConvertReferenceCommand(ReferenceParser parser, ComponentTransformer transformer, CBRDAPIClient apiClient) {
        this.parser = parser;
        this.transformer = transformer;
        this.apiClient = apiClient;
    }

    public ConvertReferenceResult execute(String selectedRefXml) {
        try {
            TripitakaComponents parsed = parser.parseReference(selectedRefXml);
            TransformedComponents transformed = transformer.transform(parsed);
            String url = apiClient.convertToFirstLink(transformed);
            return ConvertReferenceResult.success(url);
        } catch (InvalidReferenceException e) {
            return ConvertReferenceResult.failure(e.getMessageKey(), e.getParams());
        } catch (TransformationException e) {
            return ConvertReferenceResult.failure(e.getMessageKey(), e.getParams());
        } catch (CBRDAPIException e) {
            return ConvertReferenceResult.failure(e.getMessageKey(), e.getParams());
        } catch (Exception e) {
            return ConvertReferenceResult.failure("error.api.connection");
        }
    }
}

