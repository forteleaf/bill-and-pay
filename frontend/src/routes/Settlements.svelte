<script lang="ts">
  // Settlements page - skeleton
</script>

<div class="settlements">
  <h1>정산 관리</h1>
  <p>정산 내역 조회 및 관리 화면입니다.</p>
  
  <div class="summary">
    <div class="summary-item">
      <span class="label">정산 대기:</span>
      <span class="value">0 건</span>
    </div>
    <div class="summary-item">
      <span class="label">정산 확정:</span>
      <span class="value">0 건</span>
    </div>
    <div class="summary-item">
      <span class="label">지급 완료:</span>
      <span class="value">0 건</span>
    </div>
  </div>
</div>

<style>
  .settlements {
    max-width: 1200px;
  }
  
  h1 {
    font-size: 2rem;
    margin-bottom: 2rem;
  }
  
  .summary {
    display: flex;
    gap: 2rem;
    background: white;
    padding: 2rem;
    border-radius: 0.5rem;
    margin-top: 1rem;
  }
  
  .summary-item {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
  }
  
  .label {
    font-size: 0.875rem;
    color: #666;
  }
  
  .value {
    font-size: 1.5rem;
    font-weight: bold;
  }
</style>
