package io.pivotal.services.dataTx.geode.qa.performance;

import nyla.solutions.core.operations.performance.BenchMarker;
import nyla.solutions.core.patterns.conversion.Converter;
import nyla.solutions.core.patterns.creational.generator.json.JsonGeneratorCreator;
import org.apache.geode.cache.Region;
import org.apache.geode.pdx.PdxInstance;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PdxPutPerfTest
{

    @Test
    void perftest()
    {
        JsonGeneratorCreator jsonGeneratorCreator = mock(JsonGeneratorCreator.class);
        Region<?,?> mockRegion = mock(Region.class);
        Converter<String, PdxInstance> mockConverter = mock(Converter.class);
        Function<PdxInstance,String> getIdFunc = mock(Function.class);

        PdxPutPerf pdxPutPerf = new PdxPutPerf(jsonGeneratorCreator,mockConverter,mockRegion,getIdFunc);

        pdxPutPerf.run();
        verify(getIdFunc).apply(any());
        verify(jsonGeneratorCreator).create();
        verify(mockRegion).put(any(),any());
    }
}