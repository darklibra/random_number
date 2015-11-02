(ns random-number.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [<! timeout]]
            [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponent defcomponentk]]
            [om-tools.dom :as dom :include-macros true]
            [om-bootstrap.button :as b]
            [om-bootstrap.input :as i]
            [om-bootstrap.modal :as md]))

(defn in? [s n] (some #(= % n) s))

(def app-state (atom {:number 80 :animation {:is? false :ids '()} :card {:draw? false :on -1 :used '()}}))

(defn cards-cur [] (om/ref-cursor (om/root-cursor app-state)))

(defn animation-cur [] (om/ref-cursor (:animation (om/root-cursor app-state))))

(defn card-cur [] (om/ref-cursor (:card (om/root-cursor app-state))))

(defn show-popup [owner]
  (let [{:keys [visible-popup? popup] :or {:visible-popup? false}} (om/get-state owner)
        {:keys [header body footer]} popup
        modal (md/modal {:header header :footer footer :close-button? false :visible? visible-popup?} body)]
    (dom/div modal)))

(defcomponent reinit-component [data owner]
  (init-state [_] {:number (:number data)})
  (render-state [_ {:keys [number]}]
    (let [init (fn [] (om/transact! (cards-cur)
                                    #(merge % {:number (:number (om/get-state owner)) :card {:on -1 :used '()}})))]
      (dom/div
        (i/input {:type               "text" :value number
                  :on-change          (fn [e] (om/set-state! owner :number (.. e -target -value)))
                  :on-key-down        #(when (= (.-key %) "Enter") (init))
                  :addon-button-after (b/button {:bs-style "primary"
                                                 :onClick  #(init)} "apply")})))
    ))

(defn reinit-popup-opt
  [owner number]
  (let [body (dom/div
               (om/build reinit-component {:number number}))
        popup-close #(om/set-state! owner :visible-popup? false)]
    {:header (dom/h4 "Change Number")
     :footer (dom/div (b/button {:on-click (fn [_] (popup-close))} "Close"))
     :body   body}))

(defcomponent card-component [data owner]
  (render [_]
    (let [id (:id data)
          card (om/observe owner (card-cur))
          is-on (= id (:on-id data))
          is-used (in? (:used card) id)
          class (str "card img-rounded" (if is-on " on") (if is-used " use"))]
      (dom/div {:class class :id id} (str (inc id))))
    ))

(defn build-card [on-id id]
  (om/build card-component {:on-id on-id :id id}))

(defn animation-start [owner on-ids]
  (do
    (om/set-state! owner :rands on-ids)
    (om/transact! (animation-cur) #(merge % {:ids '()}))))

(defn animation-next [owner rands]
  (go (<! (timeout 50))
      (om/update-state! owner #(merge % {:rands (rest rands)}))))

(defn animation-end []
  (do
    (om/transact! (card-cur) #(merge % {:draw? true}))
    (om/transact! (animation-cur) #(merge % {:is? false}))))

(defn card-anime [owner rands]
  (let [card-draw (om/observe owner (animation-cur))
        on-ids (:ids card-draw)
        is-animation? (:is? card-draw)]
    (cond (and is-animation? (not (empty? on-ids)))
          (animation-start owner on-ids)
          (and is-animation? (not (empty? rands)))
          (animation-next owner rands)
          (and is-animation?)
          (animation-end)
          )
    nil))

(defcomponent cards-component [{:keys [ids]} owner]
  (render-state [_ {:keys [rands]}]
    (dom/div
      (card-anime owner rands)
      (map (partial build-card (first rands)) ids))))

(defn build-cards [owner number]
  (dom/div {:class "col-md-8"}
           (om/build cards-component {:ids (range number)})))

(defn gen-ids-exclude-use []
  (let [used (-> @app-state :card :used)
        target (filter #(not (in? used %)) (range (-> @app-state :number)))
        target-c (count target)
        rand-fn (fn [] (rand-int target-c))]
    (map #(nth target %) (repeatedly target-c rand-fn))))

(defn gen-rand-ids []
  (let [gen-ids (gen-ids-exclude-use)]
    (om/transact! (animation-cur) #(merge % {:is? true :ids (take (min (count gen-ids) 25) gen-ids)}))))

(defn contents-btns [owner number]
  (let [set-popup (fn [opt] (om/update-state! owner #(merge % {:visible-popup? true :popup opt})))
        is-card-anime? (:is? (om/observe owner (animation-cur)))]
    (dom/div {:class "top-btn"}
             (b/button-group
               {}
               (b/button {:on-click (fn [_] (set-popup (reinit-popup-opt owner number))) :disabled? is-card-anime?} "Reinit")
               (b/button {:on-click (fn [_] (om/transact! (card-cur) #(merge % {:on -1 :used '()}))) :disabled? is-card-anime?} "Reset")
               (b/button {:on-click (fn [_] (gen-rand-ids)) :disabled? is-card-anime?} "Start")))))

(defn congratulation-popup-opt
  [owner id]
  {:header (dom/h3 "Congratulation!!")
   :footer (dom/div (b/button {:on-click (fn [_] (#(om/set-state! owner :visible-popup? false)))} "Close"))
   :body   (dom/h4 (str id))})

(defn lottery-draw [owner]
  (if (:draw? (om/observe owner (card-cur)))
    (let [id (ffirst (sort-by second > (frequencies (gen-ids-exclude-use))))
          opt (congratulation-popup-opt owner (inc id))]
      (do
        (om/transact! (card-cur) #(merge % {:draw? false :on -1 :used (conj (-> @app-state :card :used) id)}))
        (om/update-state! owner #(merge % {:visible-popup? true :popup opt}))
        ))))

(defcomponent result-component [_ owner]
  (render [_]
    (let [winner (:used (om/observe owner (card-cur)))]
      (dom/div {:class "col-md-4 y-scroll"}
               (for [x winner] (dom/div {:class "winner img-rounded"} (inc x)))))))

(defn build-result [owner number]
  (om/build result-component {}))

(defcomponent app-component [data owner]
  (render [_]
    (let [number (:number (om/observe owner (cards-cur)))]
      (dom/div
        (map #(% owner) [show-popup lottery-draw])
        (dom/div {:class "contents"}
                 (map #(% owner number) [contents-btns build-cards build-result]))
        ))
    ))

(om/root app-component app-state
         {:target (. js/document (getElementById "root"))})
