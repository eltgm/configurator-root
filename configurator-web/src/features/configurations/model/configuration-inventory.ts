export interface InventorySelection {
  id: number;
  quantity: number;
  availableQuantity: number;
}

export interface InventoryShortage extends InventorySelection {
  selectableQuantity: number;
}

export function getInventoryShortages(
  components: ReadonlyArray<InventorySelection>,
  ownAllocations: ReadonlyMap<number, number> = new Map(),
): InventoryShortage[] {
  return components.flatMap((component) => {
    const selectableQuantity =
      component.availableQuantity + (ownAllocations.get(component.id) ?? 0);
    return component.quantity > selectableQuantity ? [{ ...component, selectableQuantity }] : [];
  });
}

export function getSelectableQuantity(
  component: Pick<InventorySelection, 'id' | 'availableQuantity'>,
  ownAllocations: ReadonlyMap<number, number> = new Map(),
) {
  return component.availableQuantity + (ownAllocations.get(component.id) ?? 0);
}
