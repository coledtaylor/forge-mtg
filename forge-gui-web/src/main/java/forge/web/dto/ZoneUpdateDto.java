package forge.web.dto;

import java.util.ArrayList;
import java.util.List;

import forge.game.zone.ZoneType;
import forge.player.PlayerZoneUpdate;

/**
 * Flat DTO for zone update notifications.
 * Indicates which zones changed for a given player.
 */
public class ZoneUpdateDto {

    public int playerId;
    public List<String> updatedZones;

    public ZoneUpdateDto() {
        // Default constructor for Jackson
    }

    /**
     * Create a ZoneUpdateDto from a PlayerZoneUpdate instance.
     */
    public static ZoneUpdateDto from(final PlayerZoneUpdate pzu) {
        if (pzu == null) {
            return null;
        }

        final ZoneUpdateDto dto = new ZoneUpdateDto();
        dto.playerId = pzu.getPlayer() != null ? pzu.getPlayer().getId() : -1;
        dto.updatedZones = new ArrayList<>();
        for (final ZoneType zone : pzu.getZones()) {
            dto.updatedZones.add(zone.name());
        }
        return dto;
    }
}
