package com.easternsauce.model.item

case class Item(
  template: ItemTemplate,
  quantity: Int = 1,
  damage: Option[Int] = None,
  armor: Option[Int] = None /*, lootPile reference?*/
) {
  def itemInformation(trader: Boolean = false): String = {
    if (trader)
      (if (this.damage.nonEmpty)
         "Damage: " + damage.get + "\n"
       else "") +
        (if (this.armor.nonEmpty)
           "Armor: " + armor.get + "\n"
         else "") +
        template.description +
        "\n" + "Worth " + template.worth.get + " Gold" + "\n"
    else
      (if (this.damage.nonEmpty)
         "Damage: " + damage.get + "\n"
       else "") +
        (if (this.armor.nonEmpty)
           "Armor: " + armor.get + "\n"
         else "") +
        template.description +
        "\n" + "Worth " + (template.worth.get * 0.3).toInt + " Gold" + "\n"
  }

  def name: String = template.name
}
