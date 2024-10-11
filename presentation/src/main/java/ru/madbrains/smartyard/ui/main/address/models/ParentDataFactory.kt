package ru.madbrains.smartyard.ui.main.address.models

import ru.madbrains.smartyard.R
import ru.madbrains.smartyard.ui.main.address.models.interfaces.DisplayableItem
import ru.madbrains.smartyard.ui.main.address.models.interfaces.VideoCameraModel
import ru.madbrains.smartyard.ui.main.address.models.interfaces.Yard
import java.util.Random
import kotlin.collections.ArrayList

object ParentDataFactory {

    private val random = Random()
    private val titles = arrayListOf(
        "г. Тамбов, ул. Советская, 16, кв. 4",
        "г. Тамбов, ул. Мичуринская, 141А",
        "г. Котовск, ул. Зимняя, 20"
    )

    private fun randomTitle(): String {
        val index = random.nextInt(titles.size)
        return titles[index]
    }

    private fun randomChildren(): List<DisplayableItem> {
        val list: MutableList<DisplayableItem> =
            ArrayList()

        list.add(
            Yard().apply {
                image = R.drawable.ic_barrier
                caption = "Шлагбаум Север"
                open = kotlin.random.Random.nextBoolean()
                this.domophoneId = kotlin.random.Random.nextLong()
                this.doorId = kotlin.random.Random.nextInt()
            }
        )
        list.add(
            Yard().apply {
                caption = "Ворота Юг"
                image = R.drawable.ic_gates
                open = kotlin.random.Random.nextBoolean()
                this.domophoneId = kotlin.random.Random.nextLong()
                this.doorId = kotlin.random.Random.nextInt()
            }
        )

        list.add(
            Yard().apply {
                caption = "Подъезд ${(1..5).random()}"
                image = R.drawable.ic_porch
                open = kotlin.random.Random.nextBoolean()
                this.domophoneId = kotlin.random.Random.nextLong()
                this.doorId = kotlin.random.Random.nextInt()
            }
        )

        list.add(
            VideoCameraModel().apply {
                caption = "Видеокамеры"
                counter = (0..10).random()
            }
        )

        return list
    }

    fun getParents(count: Int): List<DisplayableItem> {
        val parents = mutableListOf<DisplayableItem>()
        repeat(count) {
            val parent = ParentModel(randomTitle(), 0, randomChildren())
            parents.add(parent)
        }
        repeat(6) {
            val parent = IssueModel(randomTitle())
            parents.add(parent)
        }
        return parents
    }
}
