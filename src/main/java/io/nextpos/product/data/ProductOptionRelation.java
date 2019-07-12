package io.nextpos.product.data;

import lombok.*;

import javax.persistence.*;

/**
 * https://www.baeldung.com/hibernate-inheritance
 *
 * https://www.baeldung.com/jpa-many-to-many
 */
@Entity(name = "product_option_relations")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "object_type", discriminatorType = DiscriminatorType.STRING)
@Data
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public abstract class ProductOptionRelation {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    protected ProductOption productOption;


    ProductOptionRelation(final ProductOption productOption) {
        this.productOption = productOption;
    }


    @Entity
    @DiscriminatorValue("product")
    @Data
    @EqualsAndHashCode(callSuper = true)
    @NoArgsConstructor
    public static class ProductOptionOfProduct extends ProductOptionRelation {

        @ManyToOne
        @EqualsAndHashCode.Exclude
        @ToString.Exclude
        private Product product;


        public ProductOptionOfProduct(final ProductOption productOption, final Product product) {
            super(productOption);
            this.product = product;

            this.product.getProductOptionOfProducts().add(this);
        }
    }

//    @Entity
//    @DiscriminatorValue("category")
//    @Data
//    @EqualsAndHashCode(callSuper = true)
//    public static class CategoryRelation extends ProductOptionRelation {
//
//
//    }
}