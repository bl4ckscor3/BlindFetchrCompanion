package bl4ckscor3.mod.blindfetchrcompanion;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

@Config(name = "blind-fetchr-companion")
public class BlindFetchrCompanionConfig implements ConfigData {
	@Comment("How a checked-off item should be displayed in the checklist.")
	CheckedItemDisplayType checkedItemDisplayType = CheckedItemDisplayType.DARKEN_AND_CHECKMARK;

	public enum CheckedItemDisplayType {
		DARKEN(true, false),
		CHECKMARK(false, true),
		DARKEN_AND_CHECKMARK(true, true);

		private final boolean darkens, showsCheckmark;

		CheckedItemDisplayType(boolean darkens, boolean showsCheckmark) {
			this.darkens = darkens;
			this.showsCheckmark = showsCheckmark;
		}

		public boolean darkens() {
			return darkens;
		}

		public boolean showsCheckmark() {
			return showsCheckmark;
		}
	}
}
