package ru.hiflif.luckyexp;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.hiflif.luckyexp.network.PacketHandler;

@Mod(LuckyEXP.MOD_ID)
public class LuckyEXP {
    public static final String MOD_ID = "luckyexp";
    public static final Logger LOGGER = LogManager.getLogger();

    public LuckyEXP() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event) {

        PacketHandler.init();
        LOGGER.info("LuckyEXP: Сетевой канал создан.");
    }

    private void doClientStuff(final FMLClientSetupEvent event) {
        KeyInit.register();
        LOGGER.info("LuckyEXP: Клавиши зарегистрированы.");
    }
}