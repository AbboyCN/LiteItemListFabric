package me.abboycn.data.nbtprocess;

import me.abboycn.data.LitematicaReader;
import me.abboycn.data.nbtprocess.processors.*;

public class NbtProcessorRegister {
    public static void registerNbtProcessor(){
        registerBlockNbtProcessors();
        registerEntityNbtProcessors();
    }

    private static void registerBlockNbtProcessors(){
        LitematicaReader.registerBlockProcessors(
                new SlabsProcessor(),
                new CandlesProcessor(),
                new PinkPetalsProcessor(),
                new SeaPickleProcessor(),
                new FlowerPotProcessor(),
                new DoorsProcessor(),
                new BedsProcessor(),
                new TallFlowersProcessor(),
                //new TallGrassProcessor(),//Unobtainable item
                //new LargeFernProcessor(),//Unobtainable item
                new TurtleEggProcessor()
        );
    }

    private static void registerEntityNbtProcessors(){
        LitematicaReader.registerEntityProcessors(
                new ItemFrameProcessor(),
                new GlowItemFrameProcessor(),
                new ArmorStandProcessor(),
                new PaintingProcessor(),
                new MinecartsProcessor(),
                new BoatsProcessor()
        );
    }
}
