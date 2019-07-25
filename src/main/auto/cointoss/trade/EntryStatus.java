package cointoss.trade;

import cointoss.trade.EntryStatus;
import cointoss.trade.EntryStatusModel;
import cointoss.util.Num;
import cointoss.util.ObservableNumProperty;
import java.lang.Override;
import java.lang.StringBuilder;
import java.lang.Throwable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.Objects;
import javax.annotation.processing.Generated;
import kiss.Signal;

/**
 * Generated model for {@link EntryStatusModel}.
 */
@Generated("Icy Manipulator")
public abstract class EntryStatus extends EntryStatusModel {

    /**
     * Deceive complier that the specified checked exception is unchecked exception.
     *
     * @param <T> A dummy type for {@link RuntimeException}.
     * @param throwable Any error.
     * @return A runtime error.
     * @throws T Dummy error to deceive compiler.
     */
    private static final <T extends Throwable> T quiet(Throwable throwable) throws T {
        throw (T) throwable;
    }

    /**
     * Create special property updater.
     *
     * @param name A target property name.
     * @return A special property updater.
     */
    private static final MethodHandle updater(String name)  {
        try {
            Field field = EntryStatus.class.getDeclaredField(name);
            field.setAccessible(true);
            return MethodHandles.lookup().unreflectSetter(field);
        } catch (Throwable e) {
            throw quiet(e);
        }
    }

    /** The final property updater. */
    private static final MethodHandle entrySizeUpdater = updater("entrySize");

    /** The final property updater. */
    private static final MethodHandle entryExecutedSizeUpdater = updater("entryExecutedSize");

    /** The final property updater. */
    private static final MethodHandle entryPriceUpdater = updater("entryPrice");

    /** The final property updater. */
    private static final MethodHandle exitSizeUpdater = updater("exitSize");

    /** The final property updater. */
    private static final MethodHandle exitExecutedSizeUpdater = updater("exitExecutedSize");

    /** The final property updater. */
    private static final MethodHandle exitPriceUpdater = updater("exitPrice");

    /** The final property updater. */
    private static final MethodHandle realizedProfitUpdater = updater("realizedProfit");

    /** The exposed property. */
    public final Num entrySize;

    /** The property customizer. */
    private final ObservableNumProperty entrySizeCustomizer = new ObservableNumProperty() {

        @Override
        public Num get() {
            return entrySize;
        }
    };

    /** The exposed property. */
    public final Num entryExecutedSize;

    /** The property customizer. */
    private final ObservableNumProperty entryExecutedSizeCustomizer = new ObservableNumProperty() {

        @Override
        public Num get() {
            return entryExecutedSize;
        }
    };

    /** The exposed property. */
    public final Num entryPrice;

    /** The property customizer. */
    private final ObservableNumProperty entryPriceCustomizer = new ObservableNumProperty() {

        @Override
        public Num get() {
            return entryPrice;
        }
    };

    /** The exposed property. */
    public final Num exitSize;

    /** The property customizer. */
    private final ObservableNumProperty exitSizeCustomizer = new ObservableNumProperty() {

        @Override
        public Num get() {
            return exitSize;
        }
    };

    /** The exposed property. */
    public final Num exitExecutedSize;

    /** The property customizer. */
    private final ObservableNumProperty exitExecutedSizeCustomizer = new ObservableNumProperty() {

        @Override
        public Num get() {
            return exitExecutedSize;
        }
    };

    /** The exposed property. */
    public final Num exitPrice;

    /** The property customizer. */
    private final ObservableNumProperty exitPriceCustomizer = new ObservableNumProperty() {

        @Override
        public Num get() {
            return exitPrice;
        }
    };

    /** The exposed property. */
    public final Num realizedProfit;

    /** The property customizer. */
    private final ObservableNumProperty realizedProfitCustomizer = new ObservableNumProperty() {

        @Override
        public Num get() {
            return realizedProfit;
        }
    };

    /**
     * HIDE CONSTRUCTOR
     */
    protected EntryStatus() {
        this.entrySize = super.entrySize();
        this.entryExecutedSize = super.entryExecutedSize();
        this.entryPrice = super.entryPrice();
        this.exitSize = super.exitSize();
        this.exitExecutedSize = super.exitExecutedSize();
        this.exitPrice = super.exitPrice();
        this.realizedProfit = super.realizedProfit();
    }

    /**
     * A total size of entry orders.
     *  
     *  @return A total size of entry orders.
     */
    @Override
    public final Num entrySize() {
        return this.entrySize;
    }

    /**
     * Provide classic getter API.
     *
     * @return A value of entrySize property.
     */
    @SuppressWarnings("unused")
    private final Num getEntrySize() {
        return this.entrySize;
    }

    /**
     * Provide classic setter API.
     *
     * @paran value A new value of entrySize property to assign.
     */
    final void setEntrySize(Num value) {
        if (value == null) {
            value = super.entrySize();
        }
        try {
            entrySizeUpdater.invoke(this, value);
            entrySizeCustomizer.accept(this.entrySize);
        } catch (Throwable e) {
            throw quiet(e);
        }
    }

    /**
     * Observe property diff.
     *  
     *  @return
     */
    public final Signal<Num> observeEntrySizeDiff() {
        return entrySizeCustomizer.observe$Diff();
    }

    /**
     * Observe property modification.
     *  
     *  @return
     */
    public final Signal<Num> observeEntrySize() {
        return entrySizeCustomizer.observe$();
    }

    /**
     * Observe property modification with the current value.
     *  
     *  @return
     */
    public final Signal<Num> observeEntrySizeNow() {
        return entrySizeCustomizer.observe$Now();
    }

    /**
     * A total size of executed entry orders.
     *  
     *  @return A total size of executed entry orders.
     */
    @Override
    public final Num entryExecutedSize() {
        return this.entryExecutedSize;
    }

    /**
     * Provide classic getter API.
     *
     * @return A value of entryExecutedSize property.
     */
    @SuppressWarnings("unused")
    private final Num getEntryExecutedSize() {
        return this.entryExecutedSize;
    }

    /**
     * Provide classic setter API.
     *
     * @paran value A new value of entryExecutedSize property to assign.
     */
    final void setEntryExecutedSize(Num value) {
        if (value == null) {
            value = super.entryExecutedSize();
        }
        try {
            entryExecutedSizeUpdater.invoke(this, value);
            entryExecutedSizeCustomizer.accept(this.entryExecutedSize);
        } catch (Throwable e) {
            throw quiet(e);
        }
    }

    /**
     * Observe property diff.
     *  
     *  @return
     */
    public final Signal<Num> observeEntryExecutedSizeDiff() {
        return entryExecutedSizeCustomizer.observe$Diff();
    }

    /**
     * Observe property modification.
     *  
     *  @return
     */
    public final Signal<Num> observeEntryExecutedSize() {
        return entryExecutedSizeCustomizer.observe$();
    }

    /**
     * Observe property modification with the current value.
     *  
     *  @return
     */
    public final Signal<Num> observeEntryExecutedSizeNow() {
        return entryExecutedSizeCustomizer.observe$Now();
    }

    /**
     * An average price of executed entry orders.
     *  
     *  @return An average price of executed entry orders.
     */
    @Override
    public final Num entryPrice() {
        return this.entryPrice;
    }

    /**
     * Provide classic getter API.
     *
     * @return A value of entryPrice property.
     */
    @SuppressWarnings("unused")
    private final Num getEntryPrice() {
        return this.entryPrice;
    }

    /**
     * Provide classic setter API.
     *
     * @paran value A new value of entryPrice property to assign.
     */
    final void setEntryPrice(Num value) {
        if (value == null) {
            value = super.entryPrice();
        }
        try {
            entryPriceUpdater.invoke(this, value);
            entryPriceCustomizer.accept(this.entryPrice);
        } catch (Throwable e) {
            throw quiet(e);
        }
    }

    /**
     * Observe property diff.
     *  
     *  @return
     */
    public final Signal<Num> observeEntryPriceDiff() {
        return entryPriceCustomizer.observe$Diff();
    }

    /**
     * Observe property modification.
     *  
     *  @return
     */
    public final Signal<Num> observeEntryPrice() {
        return entryPriceCustomizer.observe$();
    }

    /**
     * Observe property modification with the current value.
     *  
     *  @return
     */
    public final Signal<Num> observeEntryPriceNow() {
        return entryPriceCustomizer.observe$Now();
    }

    /**
     * A total size of exit orders.
     *  
     *  @return A total size of exit orders.
     */
    @Override
    public final Num exitSize() {
        return this.exitSize;
    }

    /**
     * Provide classic getter API.
     *
     * @return A value of exitSize property.
     */
    @SuppressWarnings("unused")
    private final Num getExitSize() {
        return this.exitSize;
    }

    /**
     * Provide classic setter API.
     *
     * @paran value A new value of exitSize property to assign.
     */
    final void setExitSize(Num value) {
        if (value == null) {
            value = super.exitSize();
        }
        try {
            exitSizeUpdater.invoke(this, value);
            exitSizeCustomizer.accept(this.exitSize);
        } catch (Throwable e) {
            throw quiet(e);
        }
    }

    /**
     * Observe property diff.
     *  
     *  @return
     */
    public final Signal<Num> observeExitSizeDiff() {
        return exitSizeCustomizer.observe$Diff();
    }

    /**
     * Observe property modification.
     *  
     *  @return
     */
    public final Signal<Num> observeExitSize() {
        return exitSizeCustomizer.observe$();
    }

    /**
     * Observe property modification with the current value.
     *  
     *  @return
     */
    public final Signal<Num> observeExitSizeNow() {
        return exitSizeCustomizer.observe$Now();
    }

    /**
     * A total size of executed exit orders.
     *  
     *  @return A total size of executed exit orders.
     */
    @Override
    public final Num exitExecutedSize() {
        return this.exitExecutedSize;
    }

    /**
     * Provide classic getter API.
     *
     * @return A value of exitExecutedSize property.
     */
    @SuppressWarnings("unused")
    private final Num getExitExecutedSize() {
        return this.exitExecutedSize;
    }

    /**
     * Provide classic setter API.
     *
     * @paran value A new value of exitExecutedSize property to assign.
     */
    final void setExitExecutedSize(Num value) {
        if (value == null) {
            value = super.exitExecutedSize();
        }
        try {
            exitExecutedSizeUpdater.invoke(this, value);
            exitExecutedSizeCustomizer.accept(this.exitExecutedSize);
        } catch (Throwable e) {
            throw quiet(e);
        }
    }

    /**
     * Observe property diff.
     *  
     *  @return
     */
    public final Signal<Num> observeExitExecutedSizeDiff() {
        return exitExecutedSizeCustomizer.observe$Diff();
    }

    /**
     * Observe property modification.
     *  
     *  @return
     */
    public final Signal<Num> observeExitExecutedSize() {
        return exitExecutedSizeCustomizer.observe$();
    }

    /**
     * Observe property modification with the current value.
     *  
     *  @return
     */
    public final Signal<Num> observeExitExecutedSizeNow() {
        return exitExecutedSizeCustomizer.observe$Now();
    }

    /**
     * An average price of executed exit orders.
     *  
     *  @return An average price of executed exit orders.
     */
    @Override
    public final Num exitPrice() {
        return this.exitPrice;
    }

    /**
     * Provide classic getter API.
     *
     * @return A value of exitPrice property.
     */
    @SuppressWarnings("unused")
    private final Num getExitPrice() {
        return this.exitPrice;
    }

    /**
     * Provide classic setter API.
     *
     * @paran value A new value of exitPrice property to assign.
     */
    final void setExitPrice(Num value) {
        if (value == null) {
            value = super.exitPrice();
        }
        try {
            exitPriceUpdater.invoke(this, value);
            exitPriceCustomizer.accept(this.exitPrice);
        } catch (Throwable e) {
            throw quiet(e);
        }
    }

    /**
     * Observe property diff.
     *  
     *  @return
     */
    public final Signal<Num> observeExitPriceDiff() {
        return exitPriceCustomizer.observe$Diff();
    }

    /**
     * Observe property modification.
     *  
     *  @return
     */
    public final Signal<Num> observeExitPrice() {
        return exitPriceCustomizer.observe$();
    }

    /**
     * Observe property modification with the current value.
     *  
     *  @return
     */
    public final Signal<Num> observeExitPriceNow() {
        return exitPriceCustomizer.observe$Now();
    }

    /**
     * A realized profit or loss of this entry.
     *  
     *  @return A realized profit or loss of this entry.
     */
    @Override
    public final Num realizedProfit() {
        return this.realizedProfit;
    }

    /**
     * Provide classic getter API.
     *
     * @return A value of realizedProfit property.
     */
    @SuppressWarnings("unused")
    private final Num getRealizedProfit() {
        return this.realizedProfit;
    }

    /**
     * Provide classic setter API.
     *
     * @paran value A new value of realizedProfit property to assign.
     */
    final void setRealizedProfit(Num value) {
        if (value == null) {
            value = super.realizedProfit();
        }
        try {
            realizedProfitUpdater.invoke(this, value);
            realizedProfitCustomizer.accept(this.realizedProfit);
        } catch (Throwable e) {
            throw quiet(e);
        }
    }

    /**
     * Observe property diff.
     *  
     *  @return
     */
    public final Signal<Num> observeRealizedProfitDiff() {
        return realizedProfitCustomizer.observe$Diff();
    }

    /**
     * Observe property modification.
     *  
     *  @return
     */
    public final Signal<Num> observeRealizedProfit() {
        return realizedProfitCustomizer.observe$();
    }

    /**
     * Observe property modification with the current value.
     *  
     *  @return
     */
    public final Signal<Num> observeRealizedProfitNow() {
        return realizedProfitCustomizer.observe$Now();
    }

    /**
     * Show all property values.
     *
     * @return All property values.
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("EntryStatus [");
        builder.append("entrySize=").append(entrySize).append(", ");
        builder.append("entryExecutedSize=").append(entryExecutedSize).append(", ");
        builder.append("entryPrice=").append(entryPrice).append(", ");
        builder.append("exitSize=").append(exitSize).append(", ");
        builder.append("exitExecutedSize=").append(exitExecutedSize).append(", ");
        builder.append("exitPrice=").append(exitPrice).append(", ");
        builder.append("realizedProfit=").append(realizedProfit).append("]");
        return builder.toString();
    }

    /**
     * Generates a hash code for a sequence of property values. The hash code is generated as if all the property values were placed into an array, and that array were hashed by calling Arrays.hashCode(Object[]). 
     *
     * @return A hash value of the sequence of property values.
     */
    @Override
    public int hashCode() {
        return Objects.hash(entrySize, entryExecutedSize, entryPrice, exitSize, exitExecutedSize, exitPrice, realizedProfit);
    }

    /**
     * Returns true if the all properties are equal to each other and false otherwise. Consequently, if both properties are null, true is returned and if exactly one property is null, false is returned. Otherwise, equality is determined by using the equals method of the base model. 
     *
     * @return true if the all properties are equal to each other and false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof EntryStatus == false) {
            return false;
        }

        EntryStatus other = (EntryStatus) o;
        if (!Objects.equals(entrySize, other.entrySize)) return false;
        if (!Objects.equals(entryExecutedSize, other.entryExecutedSize)) return false;
        if (!Objects.equals(entryPrice, other.entryPrice)) return false;
        if (!Objects.equals(exitSize, other.exitSize)) return false;
        if (!Objects.equals(exitExecutedSize, other.exitExecutedSize)) return false;
        if (!Objects.equals(exitPrice, other.exitPrice)) return false;
        if (!Objects.equals(realizedProfit, other.realizedProfit)) return false;
        return true;
    }

    /** The singleton builder. */
    public static final  Ìnstantiator<?> with = new Ìnstantiator();

    /**
     * Namespace for {@link EntryStatus}  builder methods.
     */
    public static class Ìnstantiator<Self extends EntryStatus & ÅssignableÅrbitrary<Self>> {

        /**
         * Create initialized {@link EntryStatus}.
         *
         * @return A initialized model.
         */
        public Self create() {
            return (Self) new Åssignable();
        }

        /**
         * Create initialized {@link EntryStatus} with entrySize property.
         *
         * @param value A value to assign.
         * @return A initialized model.
         */
        public Self entrySize(Num value) {
            return create().entrySize(value);
        }

        /**
         * Create initialized {@link EntryStatus} with entryExecutedSize property.
         *
         * @param value A value to assign.
         * @return A initialized model.
         */
        public Self entryExecutedSize(Num value) {
            return create().entryExecutedSize(value);
        }

        /**
         * Create initialized {@link EntryStatus} with entryPrice property.
         *
         * @param value A value to assign.
         * @return A initialized model.
         */
        public Self entryPrice(Num value) {
            return create().entryPrice(value);
        }

        /**
         * Create initialized {@link EntryStatus} with exitSize property.
         *
         * @param value A value to assign.
         * @return A initialized model.
         */
        public Self exitSize(Num value) {
            return create().exitSize(value);
        }

        /**
         * Create initialized {@link EntryStatus} with exitExecutedSize property.
         *
         * @param value A value to assign.
         * @return A initialized model.
         */
        public Self exitExecutedSize(Num value) {
            return create().exitExecutedSize(value);
        }

        /**
         * Create initialized {@link EntryStatus} with exitPrice property.
         *
         * @param value A value to assign.
         * @return A initialized model.
         */
        public Self exitPrice(Num value) {
            return create().exitPrice(value);
        }

        /**
         * Create initialized {@link EntryStatus} with realizedProfit property.
         *
         * @param value A value to assign.
         * @return A initialized model.
         */
        public Self realizedProfit(Num value) {
            return create().realizedProfit(value);
        }
    }

    /**
     * Property assignment API.
     */
    public static interface ÅssignableÅrbitrary<Next extends EntryStatus> {

        /**
         * Assign entrySize property.
         * 
         * @param value A new value to assign.
         * @return The next assignable model.
         */
        default Next entrySize(Num value) {
            ((EntryStatus) this).setEntrySize(value);
            return (Next) this;
        }

        /**
         * Assign entryExecutedSize property.
         * 
         * @param value A new value to assign.
         * @return The next assignable model.
         */
        default Next entryExecutedSize(Num value) {
            ((EntryStatus) this).setEntryExecutedSize(value);
            return (Next) this;
        }

        /**
         * Assign entryPrice property.
         * 
         * @param value A new value to assign.
         * @return The next assignable model.
         */
        default Next entryPrice(Num value) {
            ((EntryStatus) this).setEntryPrice(value);
            return (Next) this;
        }

        /**
         * Assign exitSize property.
         * 
         * @param value A new value to assign.
         * @return The next assignable model.
         */
        default Next exitSize(Num value) {
            ((EntryStatus) this).setExitSize(value);
            return (Next) this;
        }

        /**
         * Assign exitExecutedSize property.
         * 
         * @param value A new value to assign.
         * @return The next assignable model.
         */
        default Next exitExecutedSize(Num value) {
            ((EntryStatus) this).setExitExecutedSize(value);
            return (Next) this;
        }

        /**
         * Assign exitPrice property.
         * 
         * @param value A new value to assign.
         * @return The next assignable model.
         */
        default Next exitPrice(Num value) {
            ((EntryStatus) this).setExitPrice(value);
            return (Next) this;
        }

        /**
         * Assign realizedProfit property.
         * 
         * @param value A new value to assign.
         * @return The next assignable model.
         */
        default Next realizedProfit(Num value) {
            ((EntryStatus) this).setRealizedProfit(value);
            return (Next) this;
        }
    }

    /**
     * Internal aggregated API.
     */
    protected static interface ÅssignableAll {
    }

    /**
     * Mutable Model.
     */
    private static final class Åssignable extends EntryStatus implements ÅssignableAll, ÅssignableÅrbitrary {
    }

    /**
     * The identifier for properties.
     */
    static final class My {
        static final String EntrySize = "entrySize";
        static final String EntryExecutedSize = "entryExecutedSize";
        static final String EntryPrice = "entryPrice";
        static final String ExitSize = "exitSize";
        static final String ExitExecutedSize = "exitExecutedSize";
        static final String ExitPrice = "exitPrice";
        static final String RealizedProfit = "realizedProfit";
    }
}