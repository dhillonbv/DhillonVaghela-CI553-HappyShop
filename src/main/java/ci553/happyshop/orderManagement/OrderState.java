package ci553.happyshop.orderManagement;

/**
 * Represents the state of an order in the system.
 *
 * This enum defines the three possible states an order can be in:
 * - Ordered: The order was placed by the customer.
 * - Progressing: A picker is currently preparing the order for customer.
 * - Collected: The order has been collected by customer from picker
 *
 * Using an enum provides a type-safe and readable way to represent a fixed set of constant values,
 * which improves code clarity, reduces errors from invalid strings or integers,
 * and makes state management more explicit and maintainable across the system.
 */

public enum OrderState {
    Ordered,
    Progressing,
    Collected
}
