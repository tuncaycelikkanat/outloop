package com.nomorereels.app.ui

import java.time.LocalDate

data class MindfulQuote(
    val textTr: String,
    val textEn: String,
    val author: String
) {
    fun localized(isEnglish: Boolean): String = if (isEnglish) textEn else textTr
}

object MindfulQuotes {
    val quotes: List<MindfulQuote> = listOf(
        MindfulQuote(
            textTr = "Zamanımız az değil, çoğunu ziyan ediyoruz.",
            textEn = "It is not that we have a short time to live, but that we waste a lot of it.",
            author = "Seneca"
        ),
        MindfulQuote(
            textTr = "Zihninin gücüne odaklan, dışındaki olaylara değil. Bunu anladığında huzuru bulacaksın.",
            textEn = "You have power over your mind - not outside events. Realize this, and you will find strength.",
            author = "Marcus Aurelius"
        ),
        MindfulQuote(
            textTr = "Uyaran ile tepki arasında bir boşluk vardır. O boşlukta seçim yapma özgürlüğümüz yatar.",
            textEn = "Between stimulus and response there is a space. In that space is our power to choose.",
            author = "Viktor Frankl"
        ),
        MindfulQuote(
            textTr = "İnsanın tüm huzursuzluğu, bir odada tek başına sessizce oturamamasından kaynaklanır.",
            textEn = "All of humanity's problems stem from man's inability to sit quietly in a room alone.",
            author = "Blaise Pascal"
        ),
        MindfulQuote(
            textTr = "Bir şeyin bedeli, onun için feda ettiğin yaşam miktarıdır.",
            textEn = "The price of anything is the amount of life you exchange for it.",
            author = "Henry David Thoreau"
        ),
        MindfulQuote(
            textTr = "Huzur istiyorsan, kontrol edemediklerini serbest bırak.",
            textEn = "Freedom is the only worthy goal in life. It is won by disregarding things beyond our control.",
            author = "Epictetus"
        ),
        MindfulQuote(
            textTr = "Günlerimizi nasıl geçirdiğimiz, elbette hayatımızı nasıl geçirdiğimizdir.",
            textEn = "How we spend our days is, of course, how we spend our lives.",
            author = "Annie Dillard"
        ),
        MindfulQuote(
            textTr = "Dinginlik, tüm hareketlerin efendisidir.",
            textEn = "Silence is a source of great strength.",
            author = "Lao Tzu"
        ),
        MindfulQuote(
            textTr = "Şu an, gerçekten yaşayabileceğin tek zamandır. Gülümse ve yavaşla.",
            textEn = "Smile, breathe, and go slowly. The present moment is the only moment available to us.",
            author = "Thich Nhat Hanh"
        ),
        MindfulQuote(
            textTr = "Geleceğe ya da geçmişe değil, şu anın farkındalığına alan aç.",
            textEn = "Realize deeply that the present moment is all you will ever have.",
            author = "Eckhart Tolle"
        ),
        MindfulQuote(
            textTr = "Dikkatin nereye akarsa, hayatın oraya şekillenir.",
            textEn = "Attention is the currency of life. Spend it on what matters.",
            author = "James Clear"
        ),
        MindfulQuote(
            textTr = "Dışa bakan rüya görür, içe bakan uyanır.",
            textEn = "Who looks outside, dreams; who looks inside, awakes.",
            author = "Carl Jung"
        ),
        MindfulQuote(
            textTr = "Sessizlikte derin bir bilgelik var; gürültüyü bırak ve dinle.",
            textEn = "Silence is the language of peace; all else is poor translation.",
            author = "Mevlana"
        ),
        MindfulQuote(
            textTr = "Huzur, düşüncelerin sessizliğidir; arzuların yokluğu değil, farkındalığıdır.",
            textEn = "A calm mind cannot be bought; it is built through conscious choices every single day.",
            author = "Naval Ravikant"
        ),
        MindfulQuote(
            textTr = "Dalgaları durduramazsın ama sörf yapmayı öğrenebilirsin.",
            textEn = "You cannot stop the waves, but you can learn to surf.",
            author = "Jon Kabat-Zinn"
        ),
        MindfulQuote(
            textTr = "Her gün yeni bir başlangıçtır; dünün kayıplarına takılı kalma.",
            textEn = "Begin at once to live, and count each separate day as a separate life.",
            author = "Seneca"
        ),
        MindfulQuote(
            textTr = "Sürekli yaptığımız şey neyse biz oyuz. O halde mükemmellik bir eylem değil, bir alışkanlıktır.",
            textEn = "We are what we repeatedly do. Excellence, then, is not an act, but a habit.",
            author = "Aristoteles"
        ),
        MindfulQuote(
            textTr = "Hayat geriye doğru anlaşılır ama ileriye doğru yaşanır.",
            textEn = "Life can only be understood backwards; but it must be lived forwards.",
            author = "Søren Kierkegaard"
        ),
        MindfulQuote(
            textTr = "Seni rahatsız eden şeyler olaylar değil, o olaylar hakkındaki düşüncelerindir.",
            textEn = "Men are disturbed not by things, but by the view which they take of them.",
            author = "Epictetus"
        ),
        MindfulQuote(
            textTr = "Gereksiz olanı bırak; o zaman hem daha az yorulur hem de daha huzurlu olursun.",
            textEn = "Ask yourself at every moment: Is this necessary?",
            author = "Marcus Aurelius"
        ),
        MindfulQuote(
            textTr = "Sessizlik, insanı asla ele vermeyen gerçek bir dosttur.",
            textEn = "Silence is a true friend who never betrays.",
            author = "Konfüçyüs"
        ),
        MindfulQuote(
            textTr = "Mutluluk kendiliğinden gelmez; dikkatimizi bilinçli yönlendirdiğimizde yeşerir.",
            textEn = "Control of consciousness determines the quality of life.",
            author = "Mihaly Csikszentmihalyi"
        ),
        MindfulQuote(
            textTr = "Huzuru sadece kendinde bulabilirsin, dış dünyada değil.",
            textEn = "Nothing can bring you peace but yourself.",
            author = "Ralph Waldo Emerson"
        ),
        MindfulQuote(
            textTr = "Dünyanın en büyük ustalığı, kendine ait olabilmektir.",
            textEn = "The greatest thing in the world is to know how to belong to oneself.",
            author = "Montaigne"
        ),
        MindfulQuote(
            textTr = "Kusursuzluk, eklenecek bir şey kalmadığında değil, çıkarılacak bir şey kalmadığında elde edilir.",
            textEn = "Perfection is achieved, not when there is nothing more to add, but when there is nothing left to take away.",
            author = "Antoine de Saint-Exupéry"
        )
    )

    fun getQuoteForDay(dayOfYear: Int = LocalDate.now().dayOfYear): MindfulQuote {
        val index = Math.floorMod(dayOfYear, quotes.size)
        return quotes[index]
    }

    fun getRandomQuote(currentIndex: Int = -1): Pair<MindfulQuote, Int> {
        val nextIndex = if (quotes.size <= 1) 0 else {
            var candidate = (quotes.indices).random()
            while (candidate == currentIndex) {
                candidate = (quotes.indices).random()
            }
            candidate
        }
        return quotes[nextIndex] to nextIndex
    }
}
