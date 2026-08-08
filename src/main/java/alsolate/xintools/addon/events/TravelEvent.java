package alsolate.xintools.addon.events;

import net.minecraft.world.entity.player.Player;

public class TravelEvent extends Event {
    private final Player entity;

    public TravelEvent(Stage stage, Player entity) {
        super(stage);
        this.entity = entity;
    }

    public Player getEntity() {
        return entity;
    }
}
