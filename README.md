# Heston Model Calibration & Derivatives Hedging

Questo progetto Java implementa un framework avanzato per l'analisi dei derivati finanziari, focalizzandosi sulla calibrazione del **Modello di Heston** (modello a volatilità stocastica) e sulla sua applicazione pratica per il pricing, la replicazione di Variance Swap e strategie di Delta Hedging dinamico.

## 🎯 Obiettivi del Progetto

* **Ingestione Dati di Mercato**: Parsing di file Excel (.xls) contenenti serie storiche del DAX, superfici di volatilità implicita e tassi zero-coupon.
* **Calibrazione Giornaliera**: Recalibrazione dei 5 parametri del modello di Heston ($\nu_0, \theta, \kappa, \xi, \rho$) utilizzando l'algoritmo di **Levenberg-Marquardt**.
* **Replicazione Statica**: Implementazione della formula di Carr-Madan per la replicazione di un **Variance Swap** (VIX replica).
* **Hedging Dinamico**: Simulazione di una strategia di **Delta Hedging** per un'opzione europea, analizzando l'errore di hedging finale.

---

## 🛠️ Architettura e Classi Principali

### 1. Market Data Management
* **`MarketDataProvider`**: Il cuore dell'importazione dati. Converte i dati grezzi di Excel in oggetti `OptionSurfaceData` e `TimeSeries`.
* **`TimeSeries`**: Utility per la gestione di serie storiche, calcolo di log-returns e statistiche descrittive (media, varianza, etc.).

### 2. Modelli e Calibrazione (Heston Model)
Il progetto si appoggia alla libreria **Finmath** per la rappresentazione del modello:
* **`Exercise1`**: Esegue la calibrazione giornaliera e produce serie storiche dei parametri e dell'errore quadratico medio (RMSE).
* **`CalibratableHestonModel`**: Interfaccia per ottimizzare i parametri rispetto al mercato.

### 3. Prodotti Finanziari e Strategie
* **`Exercise2`**: Replicazione di un Variance Swap tramite integrazione numerica (Metodo dei Trapezi) dei payoff di opzioni Call e Put.
* **`Exercise3`**: Simulazione di un portafoglio di copertura. Vende un'opzione e ne gestisce il rischio acquistando/vendendo il sottostante (DAX) in base al Delta calcolato via Heston.

---

## 🔬 Dettagli Matematici

### Il Modello di Heston
Il modello assume che la volatilità non sia costante ma segua un processo stocastico:
$$dS_t = \mu S_t dt + \sqrt{V_t} S_t dW_t^S$$
$$dV_t = \kappa(\theta - V_t) dt + \xi \sqrt{V_t} dW_t^V$$
Dove $\rho$ rappresenta la correlazione tra i due processi Browniani $W^S$ e $W^V$.

### Replicazione VIX
Utilizzando la formula di Carr-Madan, il tasso equo di un Variance Swap viene replicato tramite un portafoglio continuo di opzioni OTM (Out-Of-The-Money):
$$\text{Fair Rate} = \frac{2}{T} \left[ rT - \ln\left(\frac{F}{S_0}\right) + \int_{0}^{F} \frac{P(K)}{K^2} dK + \int_{F}^{\infty} \frac{C(K)}{K^2} dK \right]$$

---

## 📊 Testing e Visualizzazione

Il progetto include diversi test (`TimeSeriesTest`, `DataImportTest`, `MainAppTest`) per verificare la corretta lettura dei dati e la generazione di grafici tramite `Plot2D` di Finmath.

### Esempio di calibrazione (`Exercise1`):
Il codice monitora la stabilità dei parametri nel tempo, un aspetto critico per il risk management:
1.  **Volatility ($\nu_0$)**: Varianza istantanea iniziale.
2.  **Theta ($\theta$)**: Varianza di lungo periodo.
3.  **Kappa ($\kappa$)**: Velocità di ritorno verso la media.
4.  **Xi ($\xi$)**: Volatilità della volatilità.
5.  **Rho ($\rho$)**: Correlazione asset/volatilità (leverage effect).

---

## ⚙️ Requisiti Tecnici

* **Linguaggio**: Java 11 o superiore.
* **Librerie Esterne**:
    * `finmath-lib`: Per i modelli Fourier e le classi stocastiche.
    * `Apache POI`: Per la lettura di file Excel (.xls / .xlsx).
    * `JFreeChart`: (Tramite Finmath) per la visualizzazione dei grafici.

---
**Sviluppato per il corso di Derivati presso l'Università degli Studi di Verona**
