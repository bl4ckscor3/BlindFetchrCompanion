package bl4ckscor3.mod.blindfetchrcompanion;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

@Config(name = "blind-fetchr-companion")
public class BlindFetchrCompanionConfig implements ConfigData {
	@Comment("How a checked-off item should be displayed in the checklist. This is a client-side setting.")
	CheckedItemDisplayType checkedItemDisplayType = CheckedItemDisplayType.DARKEN_AND_CHECKMARK;

	public enum CheckedItemDisplayType {
		DARKEN,
		CHECKMARK,
		DARKEN_AND_CHECKMARK;

		public boolean darkens() {
			return this == DARKEN || this == DARKEN_AND_CHECKMARK;
		}

		public boolean showsCheckmark() {
			return this == CHECKMARK || this == DARKEN_AND_CHECKMARK;
		}
	}
}
