package edu.wpi.leviathans.pathFinding;

import static org.junit.jupiter.api.Assertions.*;

import edu.wpi.leviathans.pathFinding.graph.Graph;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;

class MapParserTest {
    @Test
    public void parsingTest() {
        Graph testGraph =
                MapParser.parseMapToGraph(
                        new File(getClass().getResource("MapBnodes.csv").getFile()),
                        new File(getClass().getResource("MapBedges.csv").getFile()));

        Assertions.assertEquals(2150, testGraph.getNode("BCONF00102").data.get(MapParser.DATA_LABELS.X));
    }
}
