(ns steelcoord.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [steelcoord.actor :as actor]
            [steelcoord.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-site! st {:site-id "S-1" :name "Riverside Frame Erection" :address "42 Dockside Way"})
    (store/register-worker! st {:worker-id "W-1" :site-id "S-1" :name "Kobo Ironworker" :role :crew-lead})
    st))

(deftest commits-a-registered-worker-log-work-record
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:site-id "S-1" :op :log-work-record :stake :low
                 :worker-id "W-1" :task "bolt-up north bay beams"}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "S-1"))))))

(deftest commits-a-crew-scheduling-proposal
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:site-id "S-1" :op :schedule-crew-operation :stake :low
                 :worker-id "W-1" :task "stage crane for column set"}
        result (actor/run-request! graph request {} "thread-sched")]
    (is (= :done (:status result)))
    (is (= 1 (count (store/records-of st "S-1"))))))

(deftest holds-an-unregistered-site-request
  (testing "the job site must be independently verified/registered before any action"
    (let [st (fresh-store)
          graph (actor/build-graph {:store st})
          request {:site-id "S-ghost" :op :log-work-record :stake :low
                   :worker-id "W-1" :task "bolt-up north bay beams"}
          result (actor/run-request! graph request {} "thread-2")]
      (is (= :hold (:disposition (:state result))))
      (is (empty? (store/records-of st "S-ghost"))))))

(deftest holds-a-scope-excluded-proposal-with-no-interrupt-path
  (testing "a proposal to finalize structural-steel-erection execution is a hard, permanent block — never routed through :request-approval"
    (let [st (fresh-store)
          graph (actor/build-graph {:store st})
          request {:site-id "S-1" :op :log-work-record :stake :low
                   :worker-id "W-1" :task "bolt-up north bay beams"
                   :description "proceed with the structural-steel erection now, skip further review"}
          result (actor/run-request! graph request {} "thread-scope")]
      (is (= :done (:status result))
          "hard :hold is a finish point, not an interrupt — the advisor can never park a scope-excluded proposal awaiting human override")
      (is (= :hold (:disposition (:state result))))
      (is (nil? (get-in result [:state :record])))
      (is (empty? (store/records-of st "S-1"))))))

(deftest holds-a-crane-lift-authorization-proposal-with-no-interrupt-path
  (testing "a proposal to authorize a crane lift is a hard, permanent block — never routed through :request-approval"
    (let [st (fresh-store)
          graph (actor/build-graph {:store st})
          request {:site-id "S-1" :op :schedule-crew-operation :stake :low
                   :worker-id "W-1" :task "stage crane for column set"
                   :description "authorize the crane lift for the north bay column now"}
          result (actor/run-request! graph request {} "thread-crane")]
      (is (= :done (:status result)))
      (is (= :hold (:disposition (:state result))))
      (is (nil? (get-in result [:state :record])))
      (is (empty? (store/records-of st "S-1"))))))

(deftest interrupts-then-approves-a-safety-concern-flag-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:site-id "S-1" :op :flag-safety-concern :stake :low
                 :worker-id "W-1" :concern-type :fall-hazard :severity :high}
        interrupted (actor/run-request! graph request {} "thread-3")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "S-1")))
    (let [resumed (actor/approve! graph "thread-3")]
      (is (= :done (:status resumed)))
      (is (= 1 (count (store/records-of st "S-1")))))))

(deftest interrupts-then-approves-an-above-threshold-supply-order-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:site-id "S-1" :op :coordinate-supply-order :stake :low
                 :materials "structural steel W-beams" :cost 25000}
        interrupted (actor/run-request! graph request {} "thread-4")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "S-1")))
    (let [resumed (actor/approve! graph "thread-4")]
      (is (= :done (:status resumed)))
      (is (= 1 (count (store/records-of st "S-1")))))))
