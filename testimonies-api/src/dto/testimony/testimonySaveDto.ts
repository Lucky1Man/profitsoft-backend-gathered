export class TestimonySaveDto {
  witnessId: string;
  executionFactId: string;
  timestamp: Date;

  constructor(data: Required<TestimonySaveDto>) {
    this.witnessId = data.witnessId;
    this.executionFactId = data.executionFactId;
    this.timestamp = data.timestamp;
  }
}
