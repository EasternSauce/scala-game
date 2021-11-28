package layers.model_layer.gamestate.creature

case class Player(override val params: Creature.Params) extends Creature {

  override val isPlayer = true

  override def copy(params: Creature.Params): Player = {
    Player(params)
  }

  override def update(delta: Float): Player = {
    super.update(delta).asInstanceOf[Player]
  }
}
