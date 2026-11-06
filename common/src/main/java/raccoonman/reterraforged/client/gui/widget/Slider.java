package raccoonman.reterraforged.client.gui.widget;

import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

//TODO this should be cleaned up
public class Slider extends AbstractSliderButton {
    private double min;
    private double max;
	private final Component name;
	private final Format format;
	@Nullable
	private final Callback callback;
	// the value as shown, with its unit ("64 blocks", "Y 63"); without one the plain number
	@Nullable
	private Function<String, Component> valueText;
	// the position before this move, for the sliders with steps (SliderLimits.stepTowards)
	private double previous;
    
    public Slider(int x, int y, int width, int height, float initialValue, float min, float max, Component name, Format format, @Nullable Callback callback) {
        super(x, y, width, height, CommonComponents.EMPTY, 0.0);
        this.name = name;
        this.format = format;
        this.min = min;
        this.max = max;
        this.value = this.getSliderValue(initialValue);
        this.previous = this.value;
        this.callback = callback;
        this.updateMessage();
    }
    
    // no setValue here: since 1.21.11 that overrides AbstractSliderButton's (no longer private), which the mouse and
    // the arrow keys go through, and then the slider moved without applyValue, so no setting changed at all
    public double getValue() {
    	return this.value;
    }
    

    
    public double getPrevious() {
    	return this.previous;
    }
    
    public double getSliderValue(float value) {
    	return (Mth.clamp(value, this.min, this.max) - this.min) / (this.max - this.min);
    }
    
    public double lerpValue(double value) {
    	return Mth.lerp(value, this.min, this.max);
    }
    
    public double scaleValue(double value) {
        return this.format.scale(this.lerpValue(value));
    }
    
    @Override
    public void applyValue() {
    	if(this.callback != null) {
        	this.value = this.callback.apply(this, this.value);
    	}
    	this.previous = this.value;
    }

    public Slider withValueText(Function<String, Component> valueText) {
    	this.valueText = valueText;
    	this.updateMessage();
    	return this;
    }

    @Override
    protected void updateMessage() {
    	String number = this.format.getMessage(this.scaleValue(this.value));
        this.setMessage(CommonComponents.optionNameValue(this.name, this.valueText != null ? this.valueText.apply(number) : Component.literal(number)));
    }
    
    public static enum Format {
    	INT {
			@Override
			// rounded: the slider position of 3000 on 100 .. 10000 lerps back to 2999.99..., which was cut to 2999
			public double scale(double input) {
				return Math.round(input);
			}

			@Override
			public String getMessage(double input) {
				return String.valueOf(Math.round(input));
			}
		},
    	FLOAT {
			@Override
			public double scale(double input) {
				return input;
			}

			@Override
			public String getMessage(double input) {
				return String.format("%.3f", input);
			}
		};
    	
    	public abstract double scale(double input);
    	
    	public abstract String getMessage(double input);
    }
    
    public interface Callback {
    	double apply(Slider slider, double value);
    }
}