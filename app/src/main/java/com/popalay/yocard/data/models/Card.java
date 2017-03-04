package com.popalay.yocard.data.models;

import android.support.annotation.DrawableRes;
import android.support.annotation.IntDef;

import com.github.nitrico.lastadapter.StableId;
import com.popalay.yocard.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import io.card.payment.CreditCard;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class Card extends RealmObject implements StableId {

    public static final String ID = "id";
    public static final String HOLDER_NAME = "holderName";

    @PrimaryKey private long id;

    private String number;

    private String redactedNumber;

    private String holderName;

    @CardType private int type;

    @CardColor private int color;

    public Card() {
    }

    public Card(CreditCard creditCard) {
        this.number = creditCard.getFormattedCardNumber();
        this.redactedNumber = creditCard.getRedactedCardNumber();
        switch (creditCard.getCardType()) {
            case MASTERCARD:
                this.type = CARD_TYPE_MASTERCARD;
                break;
            case VISA:
                this.type = CARD_TYPE_VISA;
                break;
            case MAESTRO:
                this.type = CARD_TYPE_MAESTRO;
                break;
        }
    }

    public Card(String number, String redactedNumber, String holderName, @CardType int type) {
        this.number = number;
        this.redactedNumber = redactedNumber;
        this.holderName = holderName;
        this.type = type;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getRedactedNumber() {
        return redactedNumber;
    }

    public void setRedactedNumber(String redactedNumber) {
        this.redactedNumber = redactedNumber;
    }

    public String getHolderName() {
        return holderName;
    }

    public void setHolderName(String holderName) {
        this.holderName = holderName;
    }

    @CardType
    public int getType() {
        return type;
    }

    public void setType(@CardType int type) {
        this.type = type;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    @DrawableRes
    public int getIconRes() {
        switch (type) {
            case CARD_TYPE_MAESTRO:
                return R.drawable.ic_maestro;
            case CARD_TYPE_MASTERCARD:
                return R.drawable.ic_mastercard;
            case CARD_TYPE_VISA:
                return R.drawable.ic_visa;
            default:
                return 0;
        }
    }

    @DrawableRes
    public int getColorRes() {
        switch (color) {
            case CARD_COLOR_GREEN:
                return R.drawable.bg_card_green;
            case CARD_COLOR_PURPLE:
                return R.drawable.bg_card_purple;
            case CARD_COLOR_RED:
                return R.drawable.bg_card_red;
            default:
            case CARD_COLOR_GREY:
                return R.drawable.bg_card_grey;
        }
    }

    @Override
    public long getStableId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Card card = (Card) o;

        if (id != card.id) {
            return false;
        }
        if (type != card.type) {
            return false;
        }
        if (color != card.color) {
            return false;
        }
        if (number != null ? !number.equals(card.number) : card.number != null) {
            return false;
        }
        if (redactedNumber != null ? !redactedNumber.equals(card.redactedNumber) : card.redactedNumber != null) {
            return false;
        }
        return holderName != null ? holderName.equals(card.holderName) : card.holderName == null;

    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (number != null ? number.hashCode() : 0);
        result = 31 * result + (redactedNumber != null ? redactedNumber.hashCode() : 0);
        result = 31 * result + (holderName != null ? holderName.hashCode() : 0);
        result = 31 * result + type;
        result = 31 * result + color;
        return result;
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({CARD_TYPE_MAESTRO, CARD_TYPE_MASTERCARD, CARD_TYPE_VISA})
    public @interface CardType {}

    public static final int CARD_TYPE_MAESTRO = 0;
    public static final int CARD_TYPE_MASTERCARD = 1;
    public static final int CARD_TYPE_VISA = 2;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({CARD_COLOR_GREY, CARD_COLOR_RED, CARD_COLOR_GREEN, CARD_COLOR_PURPLE})
    public @interface CardColor {}

    public static final int CARD_COLOR_GREY = 0;
    public static final int CARD_COLOR_RED = 1;
    public static final int CARD_COLOR_GREEN = 2;
    public static final int CARD_COLOR_PURPLE = 3;

}
