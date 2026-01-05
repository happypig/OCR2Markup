package com.dila.dama.plugin.application.command;

import com.dila.dama.plugin.domain.model.TransformedComponents;
import com.dila.dama.plugin.domain.service.ComponentTransformer;
import com.dila.dama.plugin.domain.service.NumeralConverter;
import com.dila.dama.plugin.domain.service.ReferenceParser;
import com.dila.dama.plugin.infrastructure.api.CBRDAPIClient;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class ConvertReferenceCommandTest {

    @Test
    public void execute_success_returnsUrl() throws Exception {
        ReferenceParser parser = new ReferenceParser();
        ComponentTransformer transformer = new ComponentTransformer(new NumeralConverter());

        CBRDAPIClient apiClient = mock(CBRDAPIClient.class);
        when(apiClient.convertToFirstLink(any(TransformedComponents.class)))
            .thenReturn("https://cbetaonline.dila.edu.tw/T1514");

        ConvertReferenceCommand command = new ConvertReferenceCommand(parser, transformer, apiClient);

        String xml = "<ref><canon>T</canon><v>25</v><w>1514</w><p>917</p></ref>";
        ConvertReferenceResult result = command.execute(xml);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getUrl()).isEqualTo("https://cbetaonline.dila.edu.tw/T1514");
        verify(apiClient, times(1)).convertToFirstLink(any(TransformedComponents.class));
    }
}

